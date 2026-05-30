<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { ElIcon, ElTooltip } from 'element-plus'
import {
  ArrowLeftBold,
  ArrowRightBold,
  Brush,
  ChatLineSquare,
  CollectionTag,
  Grid,
  MoreFilled,
  SemiSelect,
} from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    maxLength?: number
  }>(),
  {
    modelValue: '',
    placeholder: '请输入活动描述...',
    maxLength: 10000,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function isToolbarActive(
  key:
    | 'bold'
    | 'italic'
    | 'strike'
    | 'bulletList'
    | 'orderedList'
    | 'blockquote'
    | 'codeBlock'
    | 'paragraph'
    | 'heading',
  attrs?: Record<string, unknown>,
) {
  return editor.value?.isActive(key, attrs) ?? false
}

function runCommand(action: () => boolean) {
  action()
}

function setParagraph() {
  return editor.value?.chain().focus().setParagraph().run() ?? false
}

function toggleHeading(level: 2 | 3) {
  return editor.value?.chain().focus().toggleHeading({ level }).run() ?? false
}

function toggleBold() {
  return editor.value?.chain().focus().toggleBold().run() ?? false
}

function toggleItalic() {
  return editor.value?.chain().focus().toggleItalic().run() ?? false
}

function toggleStrike() {
  return editor.value?.chain().focus().toggleStrike().run() ?? false
}

function toggleBulletList() {
  return editor.value?.chain().focus().toggleBulletList().run() ?? false
}

function toggleOrderedList() {
  return editor.value?.chain().focus().toggleOrderedList().run() ?? false
}

function toggleBlockquote() {
  return editor.value?.chain().focus().toggleBlockquote().run() ?? false
}

function toggleCodeBlock() {
  return editor.value?.chain().focus().toggleCodeBlock().run() ?? false
}

function setHorizontalRule() {
  return editor.value?.chain().focus().setHorizontalRule().run() ?? false
}

function clearFormatting() {
  return editor.value?.chain().focus().clearNodes().unsetAllMarks().setParagraph().run() ?? false
}

function canUndo() {
  return editor.value?.can().chain().focus().undo().run() ?? false
}

function undo() {
  return editor.value?.chain().focus().undo().run() ?? false
}

function canRedo() {
  return editor.value?.can().chain().focus().redo().run() ?? false
}

function redo() {
  return editor.value?.chain().focus().redo().run() ?? false
}

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [2, 3] },
    }),
    Placeholder.configure({ placeholder: props.placeholder }),
  ],
  editorProps: {
    attributes: {
      class: 'rich-editor-content',
    },
  },
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    if (html.length > props.maxLength) return
    emit('update:modelValue', html)
  },
})
</script>

<template>
  <div class="rich-editor-wrapper">
    <div v-if="editor" class="rich-editor-toolbar">
      <div class="toolbar-group">
        <el-tooltip content="正文" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn toolbar-btn--wide"
            :class="{ active: isToolbarActive('paragraph') }"
            @click="runCommand(setParagraph)"
          >
            正文
          </button>
        </el-tooltip>
        <el-tooltip content="标题 2" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn toolbar-btn--wide"
            :class="{ active: isToolbarActive('heading', { level: 2 }) }"
            @click="runCommand(() => toggleHeading(2))"
          >
            H2
          </button>
        </el-tooltip>
        <el-tooltip content="标题 3" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn toolbar-btn--wide"
            :class="{ active: isToolbarActive('heading', { level: 3 }) }"
            @click="runCommand(() => toggleHeading(3))"
          >
            H3
          </button>
        </el-tooltip>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <el-tooltip content="加粗" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('bold') }"
            @click="runCommand(toggleBold)"
          >
            B
          </button>
        </el-tooltip>
        <el-tooltip content="斜体" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('italic') }"
            @click="runCommand(toggleItalic)"
          >
            I
          </button>
        </el-tooltip>
        <el-tooltip content="删除线" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('strike') }"
            @click="runCommand(toggleStrike)"
          >
            <el-icon><SemiSelect /></el-icon>
          </button>
        </el-tooltip>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <el-tooltip content="无序列表" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('bulletList') }"
            @click="runCommand(toggleBulletList)"
          >
            <el-icon><CollectionTag /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="有序列表" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('orderedList') }"
            @click="runCommand(toggleOrderedList)"
          >
            <span class="toolbar-text">1.</span>
          </button>
        </el-tooltip>
        <el-tooltip content="引用" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('blockquote') }"
            @click="runCommand(toggleBlockquote)"
          >
            <el-icon><ChatLineSquare /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="代码块" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :class="{ active: isToolbarActive('codeBlock') }"
            @click="runCommand(toggleCodeBlock)"
          >
            <el-icon><Grid /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="分隔线" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            @click="runCommand(setHorizontalRule)"
          >
            <el-icon><MoreFilled /></el-icon>
          </button>
        </el-tooltip>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group toolbar-group--end">
        <el-tooltip content="清除样式" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            @click="runCommand(clearFormatting)"
          >
            <el-icon><Brush /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="撤销" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :disabled="!canUndo()"
            @click="runCommand(undo)"
          >
            <el-icon><ArrowLeftBold /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="重做" placement="top" :show-after="120">
          <button
            type="button"
            class="toolbar-btn"
            :disabled="!canRedo()"
            @click="runCommand(redo)"
          >
            <el-icon><ArrowRightBold /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
    <EditorContent :editor="editor" class="rich-editor-body" />
  </div>
</template>

<style scoped>
.rich-editor-wrapper {
  width: 100%;
  border: 1px solid var(--jg-line);
  border-radius: 14px;
  overflow: hidden;
  background: var(--jg-surface);
  transition:
    border-color 180ms var(--jg-ease),
    box-shadow 180ms var(--jg-ease);
}
.rich-editor-wrapper:focus-within {
  border-color: var(--jg-accent);
  box-shadow: 0 0 0 3px rgba(84, 116, 106, 0.12);
}
.rich-editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
  background: var(--jg-surface-muted);
  border-bottom: 1px solid var(--jg-line);
}
.toolbar-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.toolbar-group--end {
  margin-left: auto;
}
.toolbar-divider {
  width: 1px;
  align-self: stretch;
  background: var(--jg-line);
}
.toolbar-btn {
  width: 32px;
  height: 28px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: var(--jg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background-color 160ms var(--jg-ease),
    color 160ms var(--jg-ease),
    border-color 160ms var(--jg-ease);
}
.toolbar-btn--wide {
  width: auto;
  min-width: 44px;
  padding: 0 10px;
}
.toolbar-text {
  font-size: 12px;
  font-weight: 700;
}
.toolbar-btn:hover {
  background: var(--jg-accent-soft);
  color: var(--jg-accent-deep);
}
.toolbar-btn.active {
  background: var(--jg-accent);
  color: #fff;
  border-color: var(--jg-accent);
}
.toolbar-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.rich-editor-body {
  width: 100%;
  max-height: 520px;
  overflow-y: auto;
}
.rich-editor-body :deep(.rich-editor-content) {
  padding: 14px 16px 18px;
  min-height: 320px;
  outline: none;
  font-size: 14px;
  line-height: 1.7;
  color: var(--jg-ink-soft);
}
.rich-editor-body :deep(.rich-editor-content > *:first-child) {
  margin-top: 0;
}
.rich-editor-body :deep(.rich-editor-content > *:last-child) {
  margin-bottom: 0;
}
.rich-editor-body :deep(.rich-editor-content h2) {
  font-size: 22px;
  margin: 18px 0 8px;
  line-height: 1.35;
  color: var(--jg-ink);
}
.rich-editor-body :deep(.rich-editor-content h3) {
  font-size: 18px;
  margin: 16px 0 6px;
  line-height: 1.45;
  color: var(--jg-ink);
}
.rich-editor-body :deep(.rich-editor-content ul),
.rich-editor-body :deep(.rich-editor-content ol) {
  padding-left: 22px;
  margin: 10px 0;
}
.rich-editor-body :deep(.rich-editor-content p) {
  margin: 8px 0;
}
.rich-editor-body :deep(.rich-editor-content li) {
  margin: 6px 0;
}
.rich-editor-body :deep(.rich-editor-content blockquote) {
  margin: 14px 0;
  padding: 10px 14px;
  border-left: 3px solid var(--jg-accent);
  border-radius: 0 10px 10px 0;
  background: rgba(84, 116, 106, 0.08);
  color: var(--jg-ink);
}
.rich-editor-body :deep(.rich-editor-content pre) {
  margin: 14px 0;
  padding: 14px 16px;
  border-radius: 12px;
  background: #232622;
  color: #f7f5ef;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
}
.rich-editor-body :deep(.rich-editor-content code) {
  padding: 0.15em 0.4em;
  border-radius: 6px;
  background: rgba(36, 40, 36, 0.08);
  font-size: 0.92em;
}
.rich-editor-body :deep(.rich-editor-content pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}
.rich-editor-body :deep(.rich-editor-content hr) {
  margin: 18px 0;
  border: none;
  border-top: 1px dashed var(--jg-line-strong);
}
.rich-editor-body :deep(.rich-editor-content strong) {
  color: var(--jg-ink);
}
.rich-editor-body :deep(.rich-editor-content a) {
  color: var(--jg-accent-deep);
  text-decoration: underline;
}
.rich-editor-body :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  color: #c0c4cc;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}
@media (max-width: 820px) {
  .rich-editor-toolbar {
    gap: 6px;
    padding: 8px;
  }
  .toolbar-group--end {
    margin-left: 0;
  }
  .toolbar-divider {
    display: none;
  }
  .rich-editor-body :deep(.rich-editor-content) {
    min-height: 240px;
  }
}
</style>
