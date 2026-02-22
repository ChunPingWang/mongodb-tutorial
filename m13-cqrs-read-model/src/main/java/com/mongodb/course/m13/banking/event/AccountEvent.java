package com.mongodb.course.m13.banking.event;

import com.mongodb.course.m13.shared.DomainEvent;

public sealed interface AccountEvent extends DomainEvent
        permits AccountOpened, FundsDeposited, FundsWithdrawn, FundsTransferred {
}
