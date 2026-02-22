package com.mongodb.course.m20.claim.event;

import com.mongodb.course.m20.shared.DomainEvent;

public sealed interface ClaimEvent extends DomainEvent
        permits ClaimFiled, ClaimInvestigated, ClaimAssessed,
                ClaimApproved, ClaimRejected, ClaimPaid {
}
