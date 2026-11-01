package com.atish.attendance.service;

import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.SystemSetting;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingService {

    public static final String KEY_ADMIN_VIEW_EMP_DETAILS = "ADMIN_VIEW_EMP_DETAILS";

    private final SystemSettingRepository settingRepository;

    public SystemSettingService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public boolean isEmployeeDetailsVisibleToAdmin() {
        return settingRepository.findByKey(KEY_ADMIN_VIEW_EMP_DETAILS)
                .map(setting -> "true".equalsIgnoreCase(setting.getValue()))
                .orElse(true); // default true if not set
    }

    @Transactional
    public void setEmployeeDetailsVisibleToAdmin(boolean visible) {
        SystemSetting setting = settingRepository.findByKey(KEY_ADMIN_VIEW_EMP_DETAILS)
                .orElseGet(() -> new SystemSetting(
                        KEY_ADMIN_VIEW_EMP_DETAILS,
                        "true",
                        "Controls whether Admin users are allowed to view employee details and rosters"
                ));
        setting.setValue(String.valueOf(visible));
        settingRepository.save(setting);
    }

    /**
     * Checks if the given user can view employee details:
     * - SUPER_ADMIN: Always true
     * - ADMIN: True only if global setting is true AND user's canViewEmpDetails is true
     * - Other roles: default false or based on their permissions
     */
    public boolean canUserViewEmployeeDetails(User user) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ROLE_SUPER_ADMIN) {
            return true;
        }
        if (user.getRole() == Role.ROLE_ADMIN) {
            return isEmployeeDetailsVisibleToAdmin() && user.isCanViewEmpDetails();
        }
        // Manager can view subordinates
        if (user.getRole() == Role.ROLE_MANAGER) {
            return true;
        }
        return false;
    }
}
