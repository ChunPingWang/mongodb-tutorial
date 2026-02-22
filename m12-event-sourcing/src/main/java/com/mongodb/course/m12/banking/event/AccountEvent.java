package com.mongodb.course.m12.banking.event;

import com.mongodb.course.m12.shared.DomainEvent;

public sealed interface AccountEvent extends DomainEvent
        permits AccountOpened, FundsDeposited, FundsWithdrawn, FundsTransferred {
}
