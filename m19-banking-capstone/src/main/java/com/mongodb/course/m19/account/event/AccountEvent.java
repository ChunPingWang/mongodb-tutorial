package com.mongodb.course.m19.account.event;

import com.mongodb.course.m19.shared.DomainEvent;

public sealed interface AccountEvent extends DomainEvent
        permits AccountOpened, FundsDeposited, FundsWithdrawn,
                FundsTransferredOut, FundsTransferredIn,
                InterestAccrued, AccountClosed {
}
