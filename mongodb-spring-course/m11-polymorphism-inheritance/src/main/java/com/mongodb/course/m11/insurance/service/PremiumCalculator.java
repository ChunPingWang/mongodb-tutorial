package com.mongodb.course.m11.insurance.service;

import com.mongodb.course.m11.insurance.model.AutoPolicy;
import com.mongodb.course.m11.insurance.model.HealthPolicy;
import com.mongodb.course.m11.insurance.model.LifePolicy;
import com.mongodb.course.m11.insurance.model.Policy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PremiumCalculator {

    public BigDecimal calculatePremium(Policy policy) {
        return switch (policy) {
            case AutoPolicy auto when auto.getDriverAge() < 25 ->
                    auto.getBasePremium().multiply(BigDecimal.valueOf(1.5));
            case AutoPolicy auto when "truck".equals(auto.getVehicleType()) ->
                    auto.getBasePremium().multiply(BigDecimal.valueOf(1.3));
            case AutoPolicy auto ->
                    auto.getBasePremium();
            case LifePolicy life when life.getInsuredAge() > 60 ->
                    life.getBasePremium().multiply(BigDecimal.valueOf(2.0));
            case LifePolicy life ->
                    life.getBasePremium().multiply(
                            BigDecimal.ONE.add(BigDecimal.valueOf(life.getInsuredAge())
                                    .multiply(new BigDecimal("0.02"))));
            case HealthPolicy health -> {
                var premium = health.getBasePremium();
                if (health.isHasDentalCoverage()) premium = premium.add(BigDecimal.valueOf(500));
                if (health.isHasVisionCoverage()) premium = premium.add(BigDecimal.valueOf(300));
                yield premium;
            }
        };
    }
}
