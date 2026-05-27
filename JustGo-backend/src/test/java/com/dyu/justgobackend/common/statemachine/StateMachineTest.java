package com.dyu.justgobackend.common.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineTest {

    enum State { A, B, C }
    enum Event { TO_B, TO_C, TO_A }

    record Ctx(boolean allow) {}

    StateMachine<State, Event, Ctx> machine;

    @BeforeEach
    void setUp() {
        machine = StateMachine.<State, Event, Ctx>builder()
                .add(State.A, Event.TO_B, State.B)
                .add(State.A, Event.TO_C, State.C, ctx -> ctx.allow())
                .add(State.A, Event.TO_C, State.A)
                .add(State.B, Event.TO_A, State.A)
                .build();
    }

    @Test
    void fireNormalTransition() {
        State next = machine.fire(State.A, Event.TO_B, null);
        assertEquals(State.B, next);
    }

    @Test
    void fireGuardRejectedThenFallback() {
        State next = machine.fire(State.A, Event.TO_C, new Ctx(false));
        assertEquals(State.A, next);
    }

    @Test
    void fireGuardPassed() {
        State next = machine.fire(State.A, Event.TO_C, new Ctx(true));
        assertEquals(State.C, next);
    }

    @Test
    void fireUndefinedTransitionThrows() {
        IllegalTransitionException ex = assertThrows(IllegalTransitionException.class,
                () -> machine.fire(State.B, Event.TO_C, null));
        assertTrue(ex.getMessage().contains(State.B.toString()));
        assertTrue(ex.getMessage().contains(Event.TO_C.toString()));
    }

    @Test
    void canTransitionTrue() {
        assertTrue(machine.canTransition(State.A, Event.TO_B, null));
    }

    @Test
    void canTransitionFalse() {
        assertFalse(machine.canTransition(State.B, Event.TO_C, null));
    }

    @Test
    void canTransitionGuardRejectedWithFallback() {
        assertTrue(machine.canTransition(State.A, Event.TO_C, new Ctx(false)));
    }

    @Test
    void availableTransitions() {
        var transitions = machine.availableTransitions(State.A);
        assertEquals(2, transitions.size());
        assertTrue(transitions.containsKey(Event.TO_B));
        assertTrue(transitions.containsKey(Event.TO_C));
    }

    @Test
    void availableTransitionsEmptyForStateWithoutExits() {
        var transitions = machine.availableTransitions(State.C);
        assertTrue(transitions.isEmpty());
    }

    @Nested
    class BuilderTests {
        @Test
        void builderWithNullGuardThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    StateMachine.<State, Event, Ctx>builder()
                            .add(State.A, Event.TO_B, State.B, null));
        }

        @Test
        void buildEmptyMachineIsValid() {
            StateMachine<State, Event, Ctx> empty = StateMachine
                    .<State, Event, Ctx>builder().build();
            assertThrows(IllegalTransitionException.class,
                    () -> empty.fire(State.A, Event.TO_B, null));
        }
    }

    @Nested
    class MultipleGuardsSameKey {
        StateMachine<State, Event, Ctx> m2;

        @BeforeEach
        void setUp() {
            m2 = StateMachine.<State, Event, Ctx>builder()
                    .add(State.A, Event.TO_B, State.B, ctx -> ctx.allow())
                    .add(State.A, Event.TO_B, State.C, ctx -> !ctx.allow())
                    .add(State.A, Event.TO_B, State.A)
                    .build();
        }

        @Test
        void firstGuardMatches() {
            assertEquals(State.B, m2.fire(State.A, Event.TO_B, new Ctx(true)));
        }

        @Test
        void secondGuardMatches() {
            assertEquals(State.C, m2.fire(State.A, Event.TO_B, new Ctx(false)));
        }
    }

    @Nested
    class NoContextMachine {
        StateMachine<State, Event, Void> voidMachine;

        @BeforeEach
        void setUp() {
            voidMachine = StateMachine.<State, Event, Void>builder()
                    .add(State.A, Event.TO_B, State.B)
                    .add(State.B, Event.TO_A, State.A)
                    .build();
        }

        @Test
        void fireWithoutContext() {
            assertEquals(State.B, voidMachine.fire(State.A, Event.TO_B, null));
        }
    }

    @Nested
    class AllGuardsReject {
        @Test
        void throwsWithReason() {
            var m = StateMachine.<State, Event, Ctx>builder()
                    .add(State.A, Event.TO_B, State.B, ctx -> ctx.allow())
                    .build();

            IllegalTransitionException ex = assertThrows(IllegalTransitionException.class,
                    () -> m.fire(State.A, Event.TO_B, new Ctx(false)));
            assertTrue(ex.getMessage().contains("Guard"));
        }
    }
}
