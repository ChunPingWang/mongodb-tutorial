package com.mongodb.course.m12.insurance.event;

import com.mongodb.course.m12.shared.DomainEvent;

public sealed interface ClaimEvent extends DomainEvent
        permits ClaimFiled, ClaimInvestigated, ClaimAssessed, ClaimApproved, ClaimRejected, ClaimPaid {
}
