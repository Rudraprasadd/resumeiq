package com.resumeiq.service;

import com.resumeiq.exception.QuotaExceededException;
import com.resumeiq.model.User;
import com.resumeiq.repository.AnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Enforces usage quotas for FREE tier users.
 *
 * FREE plan: 3 analyses per calendar month
 * PRO plan:  unlimited
 */
@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);
    private static final int FREE_MONTHLY_LIMIT = 3;

    private final AnalysisRepository analysisRepository;

    public UsageService(AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    /**
     * Check if user can run another analysis.
     * Throws QuotaExceededException if FREE user is at limit.
     */
    public void checkAndEnforceQuota(User user) {
        if (user.getPlan() == User.Plan.PRO) {
            return; // PRO users have unlimited access
        }

        long usedThisMonth = getUsageThisMonth(user);
        log.debug("User {} has used {}/{} analyses this month",
                user.getEmail(), usedThisMonth, FREE_MONTHLY_LIMIT);

        if (usedThisMonth >= FREE_MONTHLY_LIMIT) {
            throw new QuotaExceededException();
        }
    }

    /**
     * Returns how many analyses the user has run this calendar month.
     */
    public long getUsageThisMonth(User user) {
        YearMonth current = YearMonth.now();
        LocalDateTime from = current.atDay(1).atStartOfDay();
        LocalDateTime to   = current.atEndOfMonth().atTime(23, 59, 59);

        return analysisRepository.countByUserIdAndCreatedAtBetween(user.getId(), from, to);
    }

    /**
     * Returns remaining free analyses this month (0 for PRO).
     */
    public long getRemainingThisMonth(User user) {
        if (user.getPlan() == User.Plan.PRO) {
            return Long.MAX_VALUE; // effectively unlimited
        }
        long used = getUsageThisMonth(user);
        return Math.max(0, FREE_MONTHLY_LIMIT - used);
    }
}