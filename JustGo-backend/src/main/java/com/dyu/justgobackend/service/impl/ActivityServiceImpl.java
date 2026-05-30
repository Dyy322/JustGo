package com.dyu.justgobackend.service.impl;

import com.dyu.justgobackend.common.statemachine.StateMachine;
import com.dyu.justgobackend.dto.request.activity.ActivityPageQuery;
import com.dyu.justgobackend.dto.request.activity.AddActivityImageRequest;
import com.dyu.justgobackend.dto.request.activity.CreateActivityRequest;
import com.dyu.justgobackend.dto.request.activity.UpdateActivityRequest;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityListItemResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityPageResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse.CreatorInfo;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse.ImageInfo;
import com.dyu.justgobackend.entity.activity.Activity;
import com.dyu.justgobackend.entity.activity.ActivityCategory;
import com.dyu.justgobackend.entity.activity.ActivityImage;
import com.dyu.justgobackend.entity.activity.ActivityParticipant;
import com.dyu.justgobackend.entity.user.User;
import com.dyu.justgobackend.enums.ActivityEvent;
import com.dyu.justgobackend.enums.ActivityStatus;
import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.mapper.activity.ActivityCategoryMapper;
import com.dyu.justgobackend.mapper.activity.ActivityImageMapper;
import com.dyu.justgobackend.mapper.activity.ActivityMapper;
import com.dyu.justgobackend.mapper.activity.ActivityParticipantMapper;
import com.dyu.justgobackend.mapper.activity.ActivityTagMapper;
import com.dyu.justgobackend.mapper.activity.ActivityTagRelMapper;
import com.dyu.justgobackend.mapper.user.UserMapper;
import com.dyu.justgobackend.oss.OssService;
import com.dyu.justgobackend.security.LoginUser;
import com.dyu.justgobackend.security.UserContext;
import com.dyu.justgobackend.service.ActivityService;
import com.dyu.justgobackend.service.activity.ActivityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final int MAX_IMAGES = 9;

    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ActivityImageMapper imageMapper;
    private final ActivityTagMapper tagMapper;
    private final ActivityTagRelMapper tagRelMapper;
    private final UserMapper userMapper;
    private final OssService ossService;
    private final ActivityParticipantMapper participantMapper;
    private final StateMachine<ActivityStatus, ActivityEvent, ActivityContext> stateMachine;

    public ActivityServiceImpl(
            ActivityMapper activityMapper,
            ActivityCategoryMapper categoryMapper,
            ActivityImageMapper imageMapper,
            ActivityTagMapper tagMapper,
            ActivityTagRelMapper tagRelMapper,
            UserMapper userMapper,
            OssService ossService,
            ActivityParticipantMapper participantMapper,
            StateMachine<ActivityStatus, ActivityEvent, ActivityContext> stateMachine) {
        this.activityMapper = activityMapper;
        this.categoryMapper = categoryMapper;
        this.imageMapper = imageMapper;
        this.tagMapper = tagMapper;
        this.tagRelMapper = tagRelMapper;
        this.userMapper = userMapper;
        this.ossService = ossService;
        this.participantMapper = participantMapper;
        this.stateMachine = stateMachine;
    }

    @Override
    @Transactional
    public ActivityDetailResponse create(CreateActivityRequest request) {
        requireCategoryExists(request.categoryId());
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            requireTagsExist(request.tagIds());
        }

        Activity activity = new Activity();
        activity.setCreatorId(currentUserId());
        activity.setCategoryId(request.categoryId());
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setCoverImage(request.coverImage());
        activity.setLocationName(request.locationName());
        activity.setLatitude(request.latitude());
        activity.setLongitude(request.longitude());
        activity.setAddress(request.address());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setMaxParticipants(request.maxParticipants() != null ? request.maxParticipants() : 0);
        activity.setCurrentParticipants(0);
        activity.setStatus(ActivityStatus.RECRUITING.code());
        activity.setIsFeatured(0);
        activity.setViewCount(0L);
        activityMapper.insert(activity);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            tagRelMapper.insertBatch(activity.getId(), request.tagIds());
        }

        return buildDetail(activity);
    }

    @Override
    public ActivityPageResponse<ActivityListItemResponse> list(ActivityPageQuery query) {
        int offset = (query.page() - 1) * query.size();
        List<ActivityListItemResponse> items = activityMapper.selectPageWithDetails(
                query.categoryId(), query.status(), query.keyword(), offset, query.size());
        long total = activityMapper.countPage(query.categoryId(), query.status(), query.keyword());

        if (!items.isEmpty()) {
            enrichTags(items);
            enrichCoverUrls(items);
        }

        return new ActivityPageResponse<>(items, total, query.page(), query.size());
    }

    @Override
    public ActivityDetailResponse detail(Long activityId) {
        Activity activity = requireActivity(activityId);
        activityMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .setSql("view_count = view_count + 1"));
        activity.setViewCount(activity.getViewCount() != null ? activity.getViewCount() + 1 : 1);
        return buildDetail(activity);
    }

    @Override
    @Transactional
    public ActivityDetailResponse update(Long activityId, UpdateActivityRequest request) {
        Activity activity = requireActivity(activityId);
        requireCreator(activity);

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId);

        boolean hasUpdate = false;
        if (StringUtils.hasText(request.title())) {
            wrapper.set(Activity::getTitle, request.title());
            hasUpdate = true;
        }
        if (request.description() != null) {
            wrapper.set(Activity::getDescription, request.description());
            hasUpdate = true;
        }
        if (request.categoryId() != null) {
            requireCategoryExists(request.categoryId());
            wrapper.set(Activity::getCategoryId, request.categoryId());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.locationName())) {
            wrapper.set(Activity::getLocationName, request.locationName());
            hasUpdate = true;
        }
        if (request.latitude() != null) {
            wrapper.set(Activity::getLatitude, request.latitude());
            hasUpdate = true;
        }
        if (request.longitude() != null) {
            wrapper.set(Activity::getLongitude, request.longitude());
            hasUpdate = true;
        }
        if (request.address() != null) {
            wrapper.set(Activity::getAddress, request.address());
            hasUpdate = true;
        }
        if (request.startTime() != null) {
            wrapper.set(Activity::getStartTime, request.startTime());
            hasUpdate = true;
        }
        if (request.endTime() != null) {
            wrapper.set(Activity::getEndTime, request.endTime());
            hasUpdate = true;
        }
        if (request.maxParticipants() != null) {
            wrapper.set(Activity::getMaxParticipants, request.maxParticipants());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.coverImage())) {
            wrapper.set(Activity::getCoverImage, request.coverImage());
            hasUpdate = true;
        }

        if (request.tagIds() != null) {
            requireTagsExist(request.tagIds());
            tagRelMapper.deleteByActivityId(activityId);
            if (!request.tagIds().isEmpty()) {
                tagRelMapper.insertBatch(activityId, request.tagIds());
            }
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessException(400, "请至少提交一项要修改的内容");
        }

        activityMapper.update(null, wrapper);
        return buildDetail(requireActivity(activityId));
    }

    @Override
    @Transactional
    public void cancel(Long activityId) {
        Activity activity = requireActivity(activityId);
        requireCreator(activity);

        ActivityStatus current = ActivityStatus.fromCode(activity.getStatus());
        if (current == null || current == ActivityStatus.ENDED || current == ActivityStatus.CANCELLED) {
            throw new BusinessException(400, "当前状态不允许取消");
        }

        ActivityStatus next = stateMachine.fire(current, ActivityEvent.CANCEL,
                new ActivityContext(activity.getCurrentParticipants(), activity.getMaxParticipants(), true));

        activityMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .set(Activity::getStatus, next.code()));
    }

    @Override
    @Transactional
    public void republish(Long activityId) {
        Activity activity = requireActivity(activityId);
        requireCreator(activity);

        ActivityStatus current = ActivityStatus.fromCode(activity.getStatus());
        if (current != ActivityStatus.CANCELLED) {
            throw new BusinessException(400, "只有已取消的活动才能重新发布");
        }

        ActivityStatus next = stateMachine.fire(current, ActivityEvent.REPUBLISH,
                new ActivityContext(activity.getCurrentParticipants(), activity.getMaxParticipants(), true));

        activityMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .set(Activity::getStatus, next.code()));
    }

    @Override
    @Transactional
    public void join(Long activityId) {
        Long userId = currentUserId();
        Activity activity = requireActivity(activityId);

        if (participantMapper.countByActivityAndUser(activityId, userId) > 0) {
            throw new BusinessException(400, "你已报名该活动");
        }

        ActivityStatus current = ActivityStatus.fromCode(activity.getStatus());
        if (current == null || (current != ActivityStatus.RECRUITING && current != ActivityStatus.FULL && current != ActivityStatus.ONGOING)) {
            throw new BusinessException(400, "当前状态不允许报名");
        }

        var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .setSql("current_participants = current_participants + 1")
                .apply("(max_participants = 0 OR current_participants < max_participants)");
        int affected = activityMapper.update(null, updateWrapper);
        if (affected == 0) {
            throw new BusinessException(400, "活动已满员");
        }

        activity = requireActivity(activityId);
        ActivityContext ctx = new ActivityContext(activity.getCurrentParticipants(), activity.getMaxParticipants(), false);
        ActivityStatus next = stateMachine.fire(current, ActivityEvent.JOIN, ctx);

        activityMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .set(Activity::getStatus, next.code()));

        ActivityParticipant participant = new ActivityParticipant();
        participant.setActivityId(activityId);
        participant.setUserId(userId);
        participantMapper.insert(participant);
    }

    @Override
    @Transactional
    public void leave(Long activityId) {
        Long userId = currentUserId();
        Activity activity = requireActivity(activityId);

        if (participantMapper.countByActivityAndUser(activityId, userId) == 0) {
            throw new BusinessException(400, "你未报名该活动");
        }

        ActivityStatus current = ActivityStatus.fromCode(activity.getStatus());
        if (current == null || current == ActivityStatus.ENDED || current == ActivityStatus.CANCELLED) {
            throw new BusinessException(400, "当前状态不允许取消报名");
        }

        activityMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .setSql("current_participants = IF(current_participants > 0, current_participants - 1, 0)"));

        activity = requireActivity(activityId);
        ActivityContext ctx = new ActivityContext(activity.getCurrentParticipants(), activity.getMaxParticipants(), false);
        ActivityStatus next = stateMachine.fire(current, ActivityEvent.JOIN, ctx);

        activityMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .set(Activity::getStatus, next.code()));

        participantMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ActivityParticipant>()
                        .eq(ActivityParticipant::getActivityId, activityId)
                        .eq(ActivityParticipant::getUserId, userId));
    }

    @Override
    public boolean isJoined(Long activityId) {
        return UserContext.get()
                .map(loginUser -> participantMapper.countByActivityAndUser(activityId, loginUser.id()) > 0)
                .orElse(false);
    }

    @Override
    public ActivityPageResponse<ActivityListItemResponse> myActivities(String type, int page, int size) {
        Long userId = currentUserId();
        int offset = (page - 1) * size;

        if ("created".equals(type)) {
            List<ActivityListItemResponse> items = activityMapper.selectByCreatorId(userId, offset, size);
            long total = activityMapper.countByCreatorId(userId);
            enrichTags(items);
            enrichCoverUrls(items);
            return new ActivityPageResponse<>(items, total, page, size);
        }

        if ("joined".equals(type)) {
            List<Long> joinedIds = participantMapper.selectJoinedActivityIds(userId);
            if (joinedIds.isEmpty()) {
                return new ActivityPageResponse<>(Collections.emptyList(), 0, page, size);
            }
            List<ActivityListItemResponse> items = activityMapper.selectByIds(joinedIds, offset, size);
            long total = activityMapper.countByIds(joinedIds);
            enrichTags(items);
            enrichCoverUrls(items);
            return new ActivityPageResponse<>(items, total, page, size);
        }

        throw new BusinessException(400, "type 必须为 created 或 joined");
    }

    @Override
    public List<ImageInfo> listImages(Long activityId) {
        requireActivity(activityId);
        return imageMapper.selectByActivityId(activityId).stream()
                .map(img -> new ImageInfo(img.getId(), toDisplayUrl(img.getUrl()), img.getSortOrder()))
                .toList();
    }

    @Override
    @Transactional
    public List<ImageInfo> addImages(Long activityId, List<AddActivityImageRequest> requests) {
        Activity activity = requireActivity(activityId);
        requireCreator(activity);

        int existing = imageMapper.selectByActivityId(activityId).size();
        if (existing + requests.size() > MAX_IMAGES) {
            throw new BusinessException(400, "每个活动最多 " + MAX_IMAGES + " 张图片");
        }

        List<ImageInfo> result = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            ActivityImage img = new ActivityImage();
            img.setActivityId(activityId);
            img.setUrl(requests.get(i).objectKey());
            img.setSortOrder(existing + i);
            imageMapper.insert(img);
            result.add(new ImageInfo(img.getId(), toDisplayUrl(img.getUrl()), img.getSortOrder()));
        }
        return result;
    }

    @Override
    public void deleteImage(Long activityId, Long imageId) {
        Activity activity = requireActivity(activityId);
        requireCreator(activity);

        ActivityImage image = imageMapper.selectById(imageId);
        if (image == null || !Objects.equals(image.getActivityId(), activityId)) {
            throw new BusinessException(404, "图片不存在");
        }
        imageMapper.deleteById(imageId);
    }

    private String toDisplayUrl(String value) {
        if (!StringUtils.hasText(value) || value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return ossService.generatePresignedGetUrl(value);
    }

    private void enrichTags(List<ActivityListItemResponse> items) {
        if (items.isEmpty()) return;
        List<Long> activityIds = items.stream().map(ActivityListItemResponse::getId).toList();
        Map<Long, List<String>> tagMap = tagRelMapper.selectTagNamesByActivityIds(activityIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row.get("activityId")).longValue(),
                        Collectors.mapping(
                                row -> (String) row.get("tagName"),
                                Collectors.toList())));
        for (ActivityListItemResponse item : items) {
            item.setTags(tagMap.getOrDefault(item.getId(), Collections.emptyList()));
        }
    }

    private void enrichCoverUrls(List<ActivityListItemResponse> items) {
        for (ActivityListItemResponse item : items) {
            item.setCoverImage(toDisplayUrl(item.getCoverImage()));
            if (item.getCreator() != null) {
                item.getCreator().setAvatar(toDisplayUrl(item.getCreator().getAvatar()));
            }
        }
    }

    private ActivityDetailResponse buildDetail(Activity activity) {
        ActivityCategory category = categoryMapper.selectById(activity.getCategoryId());
        User creator = userMapper.selectById(activity.getCreatorId());

        List<ImageInfo> images = imageMapper.selectByActivityId(activity.getId()).stream()
                .map(img -> new ImageInfo(img.getId(), toDisplayUrl(img.getUrl()), img.getSortOrder()))
                .toList();

        List<String> tags = activityMapper.selectTagsByActivityId(activity.getId());

        return new ActivityDetailResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                toDisplayUrl(activity.getCoverImage()),
                activity.getLocationName(),
                activity.getLatitude(),
                activity.getLongitude(),
                activity.getAddress(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getMaxParticipants(),
                activity.getCurrentParticipants(),
                activity.getStatus(),
                activity.getCategoryId(),
                category != null ? category.getName() : null,
                images,
                tags,
                creator != null ? new CreatorInfo(creator.getId(), creator.getNickname(), toDisplayUrl(creator.getAvatar())) : null,
                activity.getViewCount(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }

    private Activity requireActivity(Long id) {
        return Optional.ofNullable(activityMapper.selectDetailById(id))
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));
    }

    private void requireCreator(Activity activity) {
        if (!Objects.equals(activity.getCreatorId(), currentUserId())) {
            throw new BusinessException(403, "只能操作自己创建的活动");
        }
    }

    private void requireCategoryExists(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(400, "分类不存在");
        }
    }

    private void requireTagsExist(List<Long> tagIds) {
        for (Long tagId : tagIds) {
            if (tagMapper.selectById(tagId) == null) {
                throw new BusinessException(400, "标签不存在: " + tagId);
            }
        }
    }

    private Long currentUserId() {
        return UserContext.get()
                .map(LoginUser::id)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
    }
}
