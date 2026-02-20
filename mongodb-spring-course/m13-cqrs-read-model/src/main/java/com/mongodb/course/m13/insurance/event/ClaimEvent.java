package com.mongodb.course.m13.insurance.event;

import com.mongodb.course.m13.shared.DomainEvent;

public sealed interface ClaimEvent extends DomainEvent
        permits ClaimFiled, ClaimInvestigated, ClaimAssessed, ClaimApproved, ClaimRejected, ClaimPaid {
}
