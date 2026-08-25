package com.zswy.shipsupply.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean userExists(String username, String phone) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE username = ? OR phone = ?",
            Integer.class,
            username,
            phone
        );
        return count != null && count > 0;
    }

    public long insertCompany(
        String companyName,
        String companyType,
        String contactName,
        String contactPhone,
        String contactEmail,
        String status
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO company
                  (company_name, company_type, contact_name, contact_phone, contact_email, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, companyName);
            statement.setString(2, companyType);
            statement.setString(3, contactName);
            statement.setString(4, contactPhone);
            statement.setString(5, contactEmail);
            statement.setString(6, status);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public long insertUser(long companyId, String username, String phone, String passwordHash, String userType, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO sys_user (company_id, username, phone, password_hash, user_type, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, companyId);
            statement.setString(2, username);
            statement.setString(3, phone);
            statement.setString(4, passwordHash);
            statement.setString(5, userType);
            statement.setString(6, status);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void saveToken(String token, long userId, LocalDateTime expiresAt) {
        jdbcTemplate.update(
            """
            INSERT INTO sys_auth_token (token, user_id, expires_at, status)
            VALUES (?, ?, ?, 'ACTIVE')
            ON DUPLICATE KEY UPDATE
              user_id = VALUES(user_id),
              expires_at = VALUES(expires_at),
              status = 'ACTIVE',
              last_used_at = NULL
            """,
            token,
            userId,
            expiresAt
        );
    }

    public Optional<Long> findValidTokenUserId(String token) {
        List<Long> userIds = jdbcTemplate.query(
            """
            SELECT user_id
            FROM sys_auth_token
            WHERE token = ?
              AND status = 'ACTIVE'
              AND expires_at > CURRENT_TIMESTAMP
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("user_id"),
            token
        );
        if (userIds.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update(
            "UPDATE sys_auth_token SET last_used_at = CURRENT_TIMESTAMP WHERE token = ?",
            token
        );
        return Optional.of(userIds.get(0));
    }

    public void assignRole(long userId, String roleCode) {
        jdbcTemplate.update(
            """
            INSERT IGNORE INTO sys_user_role (user_id, role_id)
            SELECT ?, id FROM sys_role WHERE role_code = ? AND enabled = 1
            """,
            userId,
            roleCode
        );
    }

    public void assignCompanyRole(long userId, long companyId, String roleCode) {
        jdbcTemplate.update(
            """
            INSERT IGNORE INTO sys_user_role (user_id, role_id)
            SELECT ?, id
            FROM sys_role
            WHERE company_id = ?
              AND role_code = ?
              AND enabled = 1
            """,
            userId,
            companyId,
            roleCode
        );
    }

    public void markCompanyOwner(long userId) {
        jdbcTemplate.update(
            "UPDATE sys_user SET is_company_owner = 1, created_by = ? WHERE id = ?",
            userId,
            userId
        );
    }

    public void replaceCompanySupplierServiceTypes(long companyId, List<String> serviceTypes) {
        jdbcTemplate.update("DELETE FROM company_supplier_service_type WHERE company_id = ?", companyId);
        for (String serviceType : serviceTypes) {
            jdbcTemplate.update(
                "INSERT INTO company_supplier_service_type (company_id, service_type) VALUES (?, ?)",
                companyId,
                serviceType
            );
        }
    }

    public List<String> supplierServiceTypesForCompany(long companyId) {
        return jdbcTemplate.query(
            "SELECT service_type FROM company_supplier_service_type WHERE company_id = ? ORDER BY service_type",
            (rs, rowNum) -> rs.getString("service_type"),
            companyId
        );
    }

    public boolean isCompanyOwner(long userId) {
        Integer owner = jdbcTemplate.queryForObject(
            "SELECT is_company_owner FROM sys_user WHERE id = ?",
            Integer.class,
            userId
        );
        return owner != null && owner == 1;
    }

    public long insertCompanyMember(
        long companyId,
        String username,
        String name,
        String phone,
        String email,
        String passwordHash,
        String userType,
        long createdBy
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO sys_user
                  (company_id, username, full_name, phone, email, password_hash, user_type, status,
                   is_company_owner, created_by, invitation_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 0, ?, 'ACCEPTED')
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, companyId);
            statement.setString(2, username);
            statement.setString(3, name);
            statement.setString(4, phone);
            statement.setString(5, email);
            statement.setString(6, passwordHash);
            statement.setString(7, userType);
            statement.setLong(8, createdBy);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<CompanyMemberResponse> companyMembers(long companyId, String status, String keyword) {
        String normalizedStatus = textOrNull(status);
        String normalizedKeyword = textOrNull(keyword);
        List<CompanyMemberResponse> members = jdbcTemplate.query(
            """
            SELECT id, username, full_name, phone, email, user_type, status, is_company_owner
            FROM sys_user
            WHERE company_id = ?
              AND (? = 'ALL' OR status = ?)
              AND (
                ? IS NULL
                OR username LIKE CONCAT('%', ?, '%')
                OR full_name LIKE CONCAT('%', ?, '%')
                OR phone LIKE CONCAT('%', ?, '%')
                OR email LIKE CONCAT('%', ?, '%')
              )
            ORDER BY is_company_owner DESC, id ASC
            """,
            (rs, rowNum) -> new CompanyMemberResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getInt("is_company_owner") == 1,
                List.of(),
                List.of()
            ),
            companyId,
            normalizedStatus,
            normalizedStatus,
            normalizedKeyword,
            normalizedKeyword,
            normalizedKeyword,
            normalizedKeyword,
            normalizedKeyword
        );
        return members.stream()
            .map(member -> new CompanyMemberResponse(
                member.userId(),
                member.username(),
                member.name(),
                member.phone(),
                member.email(),
                member.userType(),
                member.status(),
                member.isCompanyOwner(),
                roleCodesForUser(member.userId()),
                userMenuPermissionKeys(member.userId())
            ))
            .toList();
    }

    public Optional<CompanyMemberResponse> findCompanyMember(long companyId, long userId) {
        List<CompanyMemberResponse> members = jdbcTemplate.query(
            """
            SELECT id, username, full_name, phone, email, user_type, status, is_company_owner
            FROM sys_user
            WHERE company_id = ? AND id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> new CompanyMemberResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getInt("is_company_owner") == 1,
                roleCodesForUser(rs.getLong("id")),
                userMenuPermissionKeys(rs.getLong("id"))
            ),
            companyId,
            userId
        );
        return members.stream().findFirst();
    }

    public List<String> roleCodesForUser(long userId) {
        return jdbcTemplate.query(
            """
            SELECT role.role_code
            FROM sys_role role
            JOIN sys_user_role user_role ON user_role.role_id = role.id
            WHERE user_role.user_id = ? AND role.enabled = 1
            ORDER BY role.id
            """,
            (rs, rowNum) -> rs.getString("role_code"),
            userId
        );
    }

    public void updateMemberStatus(long userId, String status) {
        jdbcTemplate.update(
            """
            UPDATE sys_user
            SET status = ?,
                disabled_at = CASE WHEN ? = 'DISABLED' THEN CURRENT_TIMESTAMP ELSE NULL END
            WHERE id = ?
            """,
            status,
            status,
            userId
        );
    }

    public void revokeUserTokens(long userId) {
        jdbcTemplate.update(
            "UPDATE sys_auth_token SET status = 'REVOKED' WHERE user_id = ? AND status = 'ACTIVE'",
            userId
        );
    }

    public void updatePassword(long userId, String passwordHash) {
        jdbcTemplate.update(
            "UPDATE sys_user SET password_hash = ?, last_password_reset_at = CURRENT_TIMESTAMP WHERE id = ?",
            passwordHash,
            userId
        );
    }

    public void updateMemberProfile(long userId, String name, String phone, String email) {
        jdbcTemplate.update(
            """
            UPDATE sys_user
            SET full_name = ?, phone = ?, email = ?
            WHERE id = ?
            """
            ,
            name,
            phone,
            email,
            userId
        );
    }

    public List<RoleResponse> companyAssignableRoles(long companyId) {
        List<RoleResponse> roles = jdbcTemplate.query(
            """
            SELECT role_code, role_name, role_type
            FROM sys_role
            WHERE enabled = 1
              AND company_id = ?
            ORDER BY id
            """,
            (rs, rowNum) -> new RoleResponse(
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("role_type"),
                List.of()
            ),
            companyId
        );
        return roles.stream()
            .map(role -> new RoleResponse(role.roleCode(), role.roleName(), role.roleType(), roleMenuPermissionKeys(companyId, role.roleCode())))
            .toList();
    }

    public String insertCompanyRole(long companyId, String roleCode, String roleName, String roleType) {
        String finalRoleCode = roleCode == null || roleCode.isBlank()
            ? "COMPANY_ROLE_" + companyId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()
            : roleCode.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        jdbcTemplate.update(
            """
            INSERT INTO sys_role (company_id, role_code, role_name, role_type, enabled)
            VALUES (?, ?, ?, ?, 1)
            """,
            companyId,
            finalRoleCode,
            roleName,
            roleType
        );
        return finalRoleCode;
    }

    public void updateCompanyRole(long companyId, String roleCode, String roleName, String roleType) {
        jdbcTemplate.update(
            """
            UPDATE sys_role
            SET role_name = ?, role_type = ?
            WHERE company_id = ? AND role_code = ? AND enabled = 1
            """,
            roleName,
            roleType,
            companyId,
            roleCode
        );
    }

    public boolean companyRoleExists(long companyId, String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM sys_role
            WHERE company_id = ? AND role_code = ? AND enabled = 1
            """,
            Integer.class,
            companyId,
            roleCode
        );
        return count != null && count > 0;
    }

    public boolean companyRoleInUse(long companyId, String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id
            JOIN sys_user user ON user.id = user_role.user_id
            WHERE role.company_id = ? AND role.role_code = ? AND user.company_id = ?
            """,
            Integer.class,
            companyId,
            roleCode,
            companyId
        );
        return count != null && count > 0;
    }

    public void disableCompanyRole(long companyId, String roleCode) {
        jdbcTemplate.update(
            "UPDATE sys_role SET enabled = 0 WHERE company_id = ? AND role_code = ?",
            companyId,
            roleCode
        );
    }

    public String ensureCompanyAdminRole(long companyId) {
        String roleCode = "COMPANY_ADMIN_" + companyId;
        jdbcTemplate.update(
            """
            INSERT INTO sys_role (company_id, role_code, role_name, role_type, enabled)
            VALUES (?, ?, ?, 'COMPANY_ADMIN', 1)
            ON DUPLICATE KEY UPDATE
              role_name = VALUES(role_name),
              role_type = VALUES(role_type),
              enabled = 1
            """,
            companyId,
            roleCode,
            "Company Admin"
        );
        replaceRoleMenuPermissions(companyId, roleCode, companyMenuCodes(companyId));
        return roleCode;
    }

    public Optional<AuthenticatedUser> findUserByAccount(String account) {
        List<AuthenticatedUser> users = jdbcTemplate.query(
            """
            SELECT id, username, phone, full_name, email, password_hash, user_type, status, company_id
            FROM sys_user
            WHERE username = ? OR phone = ?
            LIMIT 1
            """,
            (rs, rowNum) -> new AuthenticatedUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("phone"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getLong("company_id")
            ),
            account,
            account
        );
        return users.stream().findFirst();
    }

    public AuthenticatedUser getUserById(long userId) {
        return jdbcTemplate.queryForObject(
            """
            SELECT id, username, phone, full_name, email, password_hash, user_type, status, company_id
            FROM sys_user
            WHERE id = ?
            """,
            (rs, rowNum) -> new AuthenticatedUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("phone"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getLong("company_id")
            ),
            userId
        );
    }

    public CompanyResponse getCompany(long companyId) {
        CompanyResponse company = jdbcTemplate.queryForObject(
            """
            SELECT id, company_name, company_type, unified_social_credit_code, contact_name, contact_phone, contact_email, status
            FROM company
            WHERE id = ?
            """,
            (rs, rowNum) -> new CompanyResponse(
                rs.getLong("id"),
                rs.getString("company_name"),
                rs.getString("company_type"),
                rs.getString("unified_social_credit_code"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("contact_email"),
                rs.getString("status")
            ),
            companyId
        );
        return new CompanyResponse(
            company.id(),
            company.companyName(),
            company.companyType(),
            company.unifiedSocialCreditCode(),
            company.contactName(),
            company.contactPhone(),
            company.contactEmail(),
            company.status(),
            supplierServiceTypesForCompany(companyId)
        );
    }

    public List<RoleResponse> rolesForUser(long userId) {
        return jdbcTemplate.query(
            """
            SELECT role.role_code, role.role_name, role.role_type
            FROM sys_role role
            JOIN sys_user_role user_role ON user_role.role_id = role.id
            WHERE user_role.user_id = ? AND role.enabled = 1
            ORDER BY role.id
            """,
            (rs, rowNum) -> new RoleResponse(
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("role_type")
            ),
            userId
        );
    }

    public boolean hasRole(long userId, String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM sys_role role
            JOIN sys_user_role user_role ON user_role.role_id = role.id
            WHERE user_role.user_id = ? AND role.role_code = ? AND role.enabled = 1
            """,
            Integer.class,
            userId,
            roleCode
        );
        return count != null && count > 0;
    }

    public List<PermissionResponse> permissionsForUser(long userId) {
        return jdbcTemplate.query(
            """
            SELECT DISTINCT permission.permission_code, permission.permission_name,
                   permission.permission_type, permission.resource_code, permission.action_code
            FROM sys_permission permission
            LEFT JOIN sys_role_permission role_permission ON role_permission.permission_id = permission.id
            LEFT JOIN sys_user_role user_role
              ON user_role.role_id = role_permission.role_id
             AND user_role.user_id = ?
            LEFT JOIN sys_menu menu
              ON menu.required_permission = permission.permission_code
            LEFT JOIN sys_user_menu user_menu
              ON user_menu.menu_id = menu.id
             AND user_menu.user_id = ?
            WHERE permission.enabled = 1
              AND (user_role.user_id IS NOT NULL OR user_menu.user_id IS NOT NULL)
            ORDER BY permission.permission_type, permission.permission_code
            """,
            (rs, rowNum) -> new PermissionResponse(
                rs.getString("permission_code"),
                rs.getString("permission_name"),
                rs.getString("permission_type"),
                rs.getString("resource_code"),
                rs.getString("action_code")
            ),
            userId,
            userId
        );
    }

    public List<MenuResponse> menusForUser(long userId) {
        List<MenuResponse> menus = jdbcTemplate.query(
            """
            SELECT DISTINCT menu.menu_code, menu.menu_name, menu.route_path, menu.icon,
                   menu.parent_code, menu.sort_order, menu.required_permission, menu.visible_roles
            FROM sys_menu menu
            LEFT JOIN sys_permission permission
              ON permission.permission_code = menu.required_permission
            LEFT JOIN sys_role_permission role_permission
              ON role_permission.permission_id = permission.id
            LEFT JOIN sys_user_role user_role
              ON user_role.role_id = role_permission.role_id
             AND user_role.user_id = ?
            LEFT JOIN sys_user_menu user_menu
              ON user_menu.menu_id = menu.id
             AND user_menu.user_id = ?
            WHERE menu.enabled = 1
              AND (menu.required_permission IS NULL OR user_role.user_id IS NOT NULL OR user_menu.user_id IS NOT NULL)
            ORDER BY menu.sort_order, menu.menu_code
            """,
            (rs, rowNum) -> new MenuResponse(
                rs.getString("menu_code"),
                rs.getString("menu_name"),
                rs.getString("route_path"),
                rs.getString("icon"),
                rs.getString("parent_code"),
                rs.getInt("sort_order"),
                rs.getString("required_permission"),
                rs.getString("visible_roles"),
                List.of()
            ),
            userId,
            userId
        );
        AuthenticatedUser user = getUserById(userId);
        return filterMenusForCompany(menus, getCompany(user.companyId()));
    }

    public List<MenuResponse> companyPermissionMenus() {
        return jdbcTemplate.query(
            """
            SELECT menu_code, menu_name, route_path, icon, parent_code,
                   sort_order, required_permission, visible_roles
            FROM sys_menu
            WHERE enabled = 1
              AND (route_path IS NULL OR route_path NOT LIKE '/admin/%')
              AND menu_code NOT IN ('ONBOARDING_PROFILE', 'ONBOARDING_REVIEW_STATUS')
            ORDER BY sort_order, menu_code
            """,
            (rs, rowNum) -> new MenuResponse(
                rs.getString("menu_code"),
                rs.getString("menu_name"),
                rs.getString("route_path"),
                rs.getString("icon"),
                rs.getString("parent_code"),
                rs.getInt("sort_order"),
                rs.getString("required_permission"),
                rs.getString("visible_roles"),
                List.of()
            )
        );
    }

    public List<MenuResponse> companyPermissionMenus(long companyId) {
        return filterMenusForCompany(companyPermissionMenus(), getCompany(companyId));
    }

    public List<String> companyMenuCodes() {
        return jdbcTemplate.query(
            """
            SELECT menu_code
            FROM sys_menu
            WHERE enabled = 1
              AND route_path NOT LIKE '/admin/%'
              AND menu_code NOT IN ('ONBOARDING_PROFILE', 'ONBOARDING_REVIEW_STATUS')
            ORDER BY sort_order, menu_code
            """,
            (rs, rowNum) -> rs.getString("menu_code")
        );
    }

    public List<String> companyMenuCodes(long companyId) {
        return companyPermissionMenus(companyId).stream().map(MenuResponse::menuCode).toList();
    }

    public List<String> roleMenuPermissionKeys(long companyId, String roleCode) {
        return jdbcTemplate.query(
            """
            SELECT menu.menu_code
            FROM sys_menu menu
            JOIN sys_permission permission ON permission.permission_code = menu.required_permission
            JOIN sys_role_permission role_permission ON role_permission.permission_id = permission.id
            JOIN sys_role role ON role.id = role_permission.role_id
            WHERE role.company_id = ?
              AND role.role_code = ?
              AND role.enabled = 1
              AND menu.enabled = 1
            ORDER BY menu.sort_order, menu.menu_code
            """,
            (rs, rowNum) -> rs.getString("menu_code"),
            companyId,
            roleCode
        );
    }

    public List<String> roleMenuPermissionKeys(String roleCode) {
        return jdbcTemplate.query(
            """
            SELECT DISTINCT menu.menu_code, menu.sort_order
            FROM sys_menu menu
            JOIN sys_permission permission ON permission.permission_code = menu.required_permission
            JOIN sys_role_permission role_permission ON role_permission.permission_id = permission.id
            JOIN sys_role role ON role.id = role_permission.role_id
            WHERE role.role_code = ?
              AND role.enabled = 1
              AND menu.enabled = 1
            ORDER BY menu.sort_order, menu.menu_code
            """,
            (rs, rowNum) -> rs.getString("menu_code"),
            roleCode
        );
    }

    public List<String> userMenuPermissionKeys(long userId) {
        return jdbcTemplate.query(
            """
            SELECT menu.menu_code
            FROM sys_menu menu
            JOIN sys_user_menu user_menu ON user_menu.menu_id = menu.id
            WHERE user_menu.user_id = ?
              AND menu.enabled = 1
            ORDER BY menu.sort_order, menu.menu_code
            """,
            (rs, rowNum) -> rs.getString("menu_code"),
            userId
        );
    }

    public void replaceRoleMenuPermissions(long companyId, String roleCode, List<String> menuCodes) {
        Long roleId = jdbcTemplate.queryForObject(
            "SELECT id FROM sys_role WHERE company_id = ? AND role_code = ? AND enabled = 1",
            Long.class,
            companyId,
            roleCode
        );
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        for (String menuCode : menuCodes) {
            jdbcTemplate.update(
                """
                INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
                SELECT ?, permission.id
                FROM sys_menu menu
                JOIN sys_permission permission ON permission.permission_code = menu.required_permission
                WHERE menu.menu_code = ?
                  AND menu.enabled = 1
                  AND menu.route_path NOT LIKE '/admin/%'
                  AND menu.menu_code NOT IN ('ONBOARDING_PROFILE', 'ONBOARDING_REVIEW_STATUS')
                  AND permission.enabled = 1
                """,
                roleId,
                menuCode
            );
        }
    }

    public void replaceRoleMenuPermissions(String roleCode, List<String> menuCodes) {
        Long roleId = jdbcTemplate.queryForObject(
            "SELECT id FROM sys_role WHERE role_code = ? AND enabled = 1",
            Long.class,
            roleCode
        );
        jdbcTemplate.update(
            """
            DELETE role_permission
            FROM sys_role_permission role_permission
            JOIN sys_permission permission ON permission.id = role_permission.permission_id
            JOIN sys_menu menu ON menu.required_permission = permission.permission_code
            WHERE role_permission.role_id = ?
            """,
            roleId
        );
        for (String menuCode : menuCodes) {
            jdbcTemplate.update(
                """
                INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
                SELECT ?, permission.id
                FROM sys_menu menu
                JOIN sys_permission permission ON permission.permission_code = menu.required_permission
                WHERE menu.menu_code = ?
                  AND menu.enabled = 1
                  AND permission.enabled = 1
                """,
                roleId,
                menuCode
            );
        }
    }

    public void updateMenuSortOrders(List<MenuSortOrderItem> items) {
        for (MenuSortOrderItem item : items) {
            jdbcTemplate.update(
                "UPDATE sys_menu SET sort_order = ? WHERE menu_code = ?",
                item.sortOrder(),
                item.menuCode()
            );
        }
    }

    public void updateMenuStructures(List<MenuStructureItem> items) {
        for (MenuStructureItem item : items) {
            jdbcTemplate.update(
                "UPDATE sys_menu SET parent_code = ?, sort_order = ?, enabled = ? WHERE menu_code = ?",
                item.parentCode(),
                item.sortOrder(),
                Boolean.TRUE.equals(item.enabled()) ? 1 : 0,
                item.menuCode()
            );
        }
    }

    public void replaceUserMenuPermissions(long userId, List<String> menuCodes) {
        jdbcTemplate.update("DELETE FROM sys_user_menu WHERE user_id = ?", userId);
        for (String menuCode : menuCodes) {
            jdbcTemplate.update(
                """
                INSERT IGNORE INTO sys_user_menu (user_id, menu_id)
                SELECT ?, id
                FROM sys_menu
                WHERE menu_code = ?
                  AND enabled = 1
                  AND route_path NOT LIKE '/admin/%'
                  AND menu_code NOT IN ('ONBOARDING_PROFILE', 'ONBOARDING_REVIEW_STATUS')
                """,
                userId,
                menuCode
            );
        }
    }

    public List<RoleResponse> allRoles() {
        return jdbcTemplate.query(
            """
            SELECT role_code, role_name, role_type
            FROM sys_role
            WHERE enabled = 1
            ORDER BY id
            """,
            (rs, rowNum) -> new RoleResponse(
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("role_type")
            )
        );
    }

    public List<PermissionResponse> allPermissions() {
        return jdbcTemplate.query(
            """
            SELECT permission_code, permission_name, permission_type, resource_code, action_code
            FROM sys_permission
            WHERE enabled = 1
            ORDER BY permission_type, permission_code
            """,
            (rs, rowNum) -> new PermissionResponse(
                rs.getString("permission_code"),
                rs.getString("permission_name"),
                rs.getString("permission_type"),
                rs.getString("resource_code"),
                rs.getString("action_code")
            )
        );
    }

    public List<MenuResponse> allMenus() {
        return jdbcTemplate.query(
            """
            SELECT menu_code, menu_name, route_path, icon, parent_code,
                   sort_order, required_permission, visible_roles
            FROM sys_menu
            WHERE enabled = 1
            ORDER BY sort_order, menu_code
            """,
            (rs, rowNum) -> new MenuResponse(
                rs.getString("menu_code"),
                rs.getString("menu_name"),
                rs.getString("route_path"),
                rs.getString("icon"),
                rs.getString("parent_code"),
                rs.getInt("sort_order"),
                rs.getString("required_permission"),
                rs.getString("visible_roles"),
                List.of()
            )
        );
    }

    public List<AdminMenuResponse> allManageableMenus() {
        return jdbcTemplate.query(
            """
            SELECT id, menu_code, menu_name, route_path, icon, parent_code,
                   sort_order, required_permission, visible_roles, enabled
            FROM sys_menu
            ORDER BY sort_order, menu_code
            """,
            (rs, rowNum) -> new AdminMenuResponse(
                rs.getLong("id"),
                rs.getString("menu_code"),
                rs.getString("menu_name"),
                rs.getString("route_path"),
                rs.getString("icon"),
                rs.getString("parent_code"),
                rs.getInt("sort_order"),
                rs.getString("required_permission"),
                rs.getString("visible_roles"),
                rs.getInt("enabled") == 1
            )
        );
    }

    public List<MenuPermissionResponse> allMenuPermissions() {
        return jdbcTemplate.query(
            """
            SELECT id, menu_code, menu_name, parent_code, sort_order, required_permission
            FROM sys_menu
            WHERE enabled = 1
            ORDER BY sort_order, menu_code
            """,
            (rs, rowNum) -> new MenuPermissionResponse(
                rs.getLong("id"),
                rs.getString("menu_code"),
                rs.getString("menu_name"),
                rs.getString("parent_code"),
                rs.getInt("sort_order"),
                rs.getString("required_permission")
            )
        );
    }

    public List<AdminUserResponse> allUsers() {
        List<AdminUserResponse> users = jdbcTemplate.query(
            """
            SELECT user.id, user.username, user.phone, user.user_type, user.status,
                   company.company_name
            FROM sys_user user
            JOIN company company ON company.id = user.company_id
            ORDER BY user.id DESC
            """,
            (rs, rowNum) -> new AdminUserResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("phone"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getString("company_name"),
                List.of()
            )
        );
        Map<Long, List<String>> rolesByUserId = roleCodesByUserId();
        List<AdminUserResponse> withRoles = new ArrayList<>();
        for (AdminUserResponse user : users) {
            withRoles.add(new AdminUserResponse(
                user.id(),
                user.username(),
                user.phone(),
                user.userType(),
                user.status(),
                user.companyName(),
                rolesByUserId.getOrDefault(user.id(), List.of())
            ));
        }
        return withRoles;
    }

    public List<PlatformCompanyAccountsResponse> platformCompanyAccounts() {
        List<CompanyResponse> companies = jdbcTemplate.query(
            """
            SELECT id, company_name, company_type, unified_social_credit_code,
                   contact_name, contact_phone, contact_email, status
            FROM company
            WHERE company_type <> 'PLATFORM_ADMIN'
            ORDER BY id DESC
            """,
            (rs, rowNum) -> new CompanyResponse(
                rs.getLong("id"),
                rs.getString("company_name"),
                rs.getString("company_type"),
                rs.getString("unified_social_credit_code"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("contact_email"),
                rs.getString("status")
            )
        );
        List<PlatformCompanyAccountsResponse> result = new ArrayList<>();
        for (CompanyResponse company : companies) {
            List<PlatformAccountResponse> accounts = platformAccountsForCompany(company.id());
            int activeAccountCount = (int) accounts.stream().filter(account -> "ACTIVE".equals(account.status())).count();
            result.add(new PlatformCompanyAccountsResponse(
                company.id(),
                company.companyName(),
                company.companyType(),
                supplierServiceTypesForCompany(company.id()),
                company.status(),
                accounts.size(),
                activeAccountCount,
                accounts
            ));
        }
        return result;
    }

    public Optional<PlatformAccountResponse> findPlatformAccount(long userId) {
        return platformAccountQuery("WHERE user.id = ? AND company.company_type <> 'PLATFORM_ADMIN'", userId)
            .stream()
            .findFirst();
    }

    private List<PlatformAccountResponse> platformAccountsForCompany(long companyId) {
        return platformAccountQuery("WHERE user.company_id = ?", companyId);
    }

    private List<PlatformAccountResponse> platformAccountQuery(String whereClause, long value) {
        List<PlatformAccountResponse> accounts = jdbcTemplate.query(
            """
            SELECT user.id, user.username, user.full_name, user.phone, user.email,
                   user.user_type, user.status, user.is_company_owner, user.created_at,
                   (
                     SELECT MAX(log.created_at)
                     FROM operation_log log
                     WHERE log.operator_user_id = user.id AND log.operation_type = 'LOGIN'
                   ) AS last_login_at
            FROM sys_user user
            JOIN company company ON company.id = user.company_id
            """ + whereClause + " ORDER BY user.is_company_owner DESC, user.id ASC",
            (rs, rowNum) -> new PlatformAccountResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("user_type"),
                rs.getInt("is_company_owner") == 1 ? "REGISTERED_ADMIN" : "INTERNAL_CREATED",
                rs.getInt("is_company_owner") == 1,
                List.of(),
                rs.getString("status"),
                timestampToString(rs.getTimestamp("created_at")),
                timestampToString(rs.getTimestamp("last_login_at"))
            ),
            value
        );
        return accounts.stream()
            .map(account -> new PlatformAccountResponse(
                account.userId(),
                account.username(),
                account.name(),
                account.phone(),
                account.email(),
                account.userType(),
                account.accountSource(),
                account.isCompanyOwner(),
                roleCodesForUser(account.userId()),
                account.status(),
                account.createdAt(),
                account.lastLoginAt()
            ))
            .toList();
    }

    public List<PermissionResponse> permissionsForRole(String roleCode) {
        return jdbcTemplate.query(
            """
            SELECT permission.permission_code, permission.permission_name,
                   permission.permission_type, permission.resource_code, permission.action_code
            FROM sys_permission permission
            JOIN sys_role_permission role_permission ON role_permission.permission_id = permission.id
            JOIN sys_role role ON role.id = role_permission.role_id
            WHERE role.role_code = ? AND permission.enabled = 1
            ORDER BY permission.permission_type, permission.permission_code
            """,
            (rs, rowNum) -> new PermissionResponse(
                rs.getString("permission_code"),
                rs.getString("permission_name"),
                rs.getString("permission_type"),
                rs.getString("resource_code"),
                rs.getString("action_code")
            ),
            roleCode
        );
    }

    public void replaceRolePermissions(String roleCode, List<String> permissionCodes) {
        Long roleId = jdbcTemplate.queryForObject(
            "SELECT id FROM sys_role WHERE role_code = ?",
            Long.class,
            roleCode
        );
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                """
                INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
                SELECT ?, id FROM sys_permission WHERE permission_code = ? AND enabled = 1
                """,
                roleId,
                permissionCode
            );
        }
    }

    public void replaceUserRoles(long userId, List<String> roleCodes) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (String roleCode : roleCodes) {
            assignRole(userId, roleCode);
        }
    }

    public void replaceCompanyUserRoles(long userId, long companyId, List<String> roleCodes) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (String roleCode : roleCodes) {
            assignCompanyRole(userId, companyId, roleCode);
        }
    }

    public void updateCompanyProfile(
        long companyId,
        String companyType,
        String companyName,
        String unifiedSocialCreditCode,
        String contactName,
        String contactPhone,
        String contactEmail,
        String status,
        String reviewComment
    ) {
        jdbcTemplate.update(
            """
            UPDATE company
            SET company_type = ?,
                company_name = ?,
                unified_social_credit_code = ?,
                contact_name = ?,
                contact_phone = ?,
                contact_email = ?,
                status = ?,
                review_comment = ?
            WHERE id = ?
            """,
            companyType,
            companyName,
            unifiedSocialCreditCode,
            contactName,
            contactPhone,
            contactEmail,
            status,
            reviewComment,
            companyId
        );
    }

    public void updateUserStatusAndType(long userId, String status, String userType) {
        jdbcTemplate.update(
            "UPDATE sys_user SET status = ?, user_type = ? WHERE id = ?",
            status,
            userType,
            userId
        );
    }

    public void updateCompanyStatus(long companyId, String status, String reviewComment) {
        jdbcTemplate.update(
            "UPDATE company SET status = ?, review_comment = ? WHERE id = ?",
            status,
            reviewComment,
            companyId
        );
    }

    public void clearUserRoles(long userId) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
    }

    public String insertFile(Long uploaderUserId, String originalName, String storagePath, String contentType, long fileSize) {
        String fileId = "FILE-" + UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO sys_file
              (file_id, uploader_user_id, original_name, storage_path, content_type, file_size, status)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
            """,
            fileId,
            uploaderUserId,
            originalName,
            storagePath,
            contentType,
            fileSize
        );
        return fileId;
    }

    public Optional<FileUploadResponse> findFile(String fileId) {
        List<FileUploadResponse> files = jdbcTemplate.query(
            """
            SELECT file_id, original_name, content_type, file_size, storage_path
            FROM sys_file
            WHERE file_id = ?
            """,
            (rs, rowNum) -> new FileUploadResponse(
                rs.getString("file_id"),
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getLong("file_size"),
                rs.getString("storage_path")
            ),
            fileId
        );
        return files.stream().findFirst();
    }

    public Optional<StoredFileResponse> findStoredFile(String fileId) {
        List<StoredFileResponse> files = jdbcTemplate.query(
            """
            SELECT file.file_id, file.original_name, file.content_type, file.file_size,
                   file.storage_path, file.uploader_user_id,
                   COALESCE(qualification.company_id, quality_selection.company_id) AS qualification_company_id
            FROM sys_file file
            LEFT JOIN company_qualification qualification
              ON qualification.file_id = file.file_id
            LEFT JOIN shop_sku_quality_selection quality_selection
              ON quality_selection.inspection_report_file_id = file.file_id
             AND quality_selection.status = 'ACTIVE'
            WHERE file.file_id = ?
            LIMIT 1
            """,
            (rs, rowNum) -> new StoredFileResponse(
                rs.getString("file_id"),
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getLong("file_size"),
                rs.getString("storage_path"),
                nullableLong(rs, "uploader_user_id"),
                nullableLong(rs, "qualification_company_id")
            ),
            fileId
        );
        return files.stream().findFirst();
    }

    public Optional<StoredFileResponse> findPublicSupplierQualificationFile(long companyId, long qualificationId) {
        List<StoredFileResponse> files = jdbcTemplate.query(
            """
            SELECT file.file_id, file.original_name, file.content_type, file.file_size,
                   file.storage_path, file.uploader_user_id,
                   qualification.company_id AS qualification_company_id
            FROM company_qualification qualification
            JOIN company company ON company.id = qualification.company_id
            JOIN sys_file file ON file.file_id = qualification.file_id
            WHERE qualification.company_id = ?
              AND qualification.id = ?
              AND company.company_type = 'SUPPLIER'
              AND company.status = 'ACTIVE'
              AND qualification.status IN ('ACTIVE', 'SUBMITTED', 'APPROVED')
              AND file.status = 'ACTIVE'
            LIMIT 1
            """,
            (rs, rowNum) -> new StoredFileResponse(
                rs.getString("file_id"),
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getLong("file_size"),
                rs.getString("storage_path"),
                nullableLong(rs, "uploader_user_id"),
                nullableLong(rs, "qualification_company_id")
            ),
            companyId,
            qualificationId
        );
        return files.stream().findFirst();
    }

    public boolean hasBusinessFileAccess(String fileId, Long companyId, boolean regulatory) {
        if (fileId == null || fileId.isBlank() || companyId == null) return false;
        String jsonNeedle = "%\"fileId\":\"" + fileId + "\"%";
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM (
              SELECT attachment.id
              FROM fulfillment_attachment attachment
              LEFT JOIN purchase_order purchase ON purchase.id = attachment.purchase_order_id
              WHERE attachment.file_id = ?
                AND (attachment.provider_company_id = ? OR purchase.buyer_company_id = ?)
              UNION ALL
              SELECT settlement.id
              FROM settlement_order settlement
              WHERE settlement.invoice_attachments_json LIKE ?
                AND (settlement.provider_company_id = ? OR settlement.buyer_company_id = ?)
              UNION ALL
              SELECT evaluation.id
              FROM service_evaluation evaluation
              WHERE evaluation.attachments_json LIKE ?
                AND (? = 1 OR evaluation.provider_company_id = ? OR evaluation.buyer_company_id = ?)
            ) accessible_file
            """, Long.class,
            fileId, companyId, companyId,
            jsonNeedle, companyId, companyId,
            jsonNeedle, regulatory ? 1 : 0, companyId, companyId
        );
        return count != null && count > 0;
    }

    public void replaceCompanyQualifications(long companyId, List<String> fileIds) {
        jdbcTemplate.update("DELETE FROM company_qualification WHERE company_id = ?", companyId);
        for (String fileId : fileIds) {
            FileUploadResponse file = findFile(fileId)
                .orElse(new FileUploadResponse(fileId, fileId, null, 0L, ""));
            jdbcTemplate.update(
                """
                INSERT INTO company_qualification
                  (company_id, file_id, file_type, file_name, status)
                VALUES (?, ?, 'BUSINESS_LICENSE', ?, 'SUBMITTED')
                """,
                companyId,
                fileId,
                file.fileName()
            );
        }
    }

    public List<QualificationFileResponse> qualificationsForCompany(long companyId) {
        return jdbcTemplate.query(
            """
            SELECT file_id, file_type, file_name, status
            FROM company_qualification
            WHERE company_id = ?
            ORDER BY id
            """,
            (rs, rowNum) -> new QualificationFileResponse(
                rs.getString("file_id"),
                rs.getString("file_type"),
                rs.getString("file_name"),
                rs.getString("status"),
                "/api/files/" + rs.getString("file_id")
            ),
            companyId
        );
    }

    public List<RegistrationReviewResponse> registrationsForReview(String status, String keyword, int page, int pageSize) {
        String normalizedStatus = textOrNull(status);
        String normalizedKeyword = textOrNull(keyword);
        int offset = (page - 1) * pageSize;
        List<RegistrationReviewResponse> registrations = jdbcTemplate.query(
            """
            SELECT user.id AS user_id, company.id AS company_id, user.username, user.phone,
                   company.company_name, company.company_type, company.contact_name,
                   company.contact_phone, company.contact_email, user.status AS account_status,
                   company.status AS company_status, company.review_comment,
                   company.updated_at AS submitted_at,
                   review_log.created_at AS reviewed_at,
                   review_log.operator_user_id AS reviewer_user_id
            FROM sys_user user
            JOIN company company ON company.id = user.company_id
            LEFT JOIN (
                SELECT log.operator_user_id, log.target_id, log.created_at
                FROM operation_log log
                JOIN (
                    SELECT target_id, MAX(created_at) AS reviewed_at
                    FROM operation_log
                    WHERE operation_type IN ('APPROVE_REGISTRATION', 'REJECT_REGISTRATION')
                    GROUP BY target_id
                ) latest
                  ON latest.target_id = log.target_id
                 AND latest.reviewed_at = log.created_at
                WHERE log.operation_type IN ('APPROVE_REGISTRATION', 'REJECT_REGISTRATION')
            ) review_log ON review_log.target_id = CAST(user.id AS CHAR)
            WHERE company.company_type <> 'PLATFORM_ADMIN'
              AND (
                user.status IN ('PROFILE_REQUIRED', 'PENDING_REVIEW', 'REJECTED', 'ACTIVE')
                OR company.status IN ('PROFILE_REQUIRED', 'PENDING_REVIEW', 'REJECTED', 'ACTIVE')
              )
              AND (? IS NULL OR user.status = ? OR company.status = ?)
                AND (
                    ? IS NULL
                    OR company.company_name LIKE CONCAT('%', ?, '%')
                    OR company.contact_name LIKE CONCAT('%', ?, '%')
                    OR company.contact_phone LIKE CONCAT('%', ?, '%')
                    OR company.contact_email LIKE CONCAT('%', ?, '%')
              )
            ORDER BY user.id DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new RegistrationReviewResponse(
                rs.getLong("user_id"),
                rs.getLong("user_id"),
                rs.getLong("company_id"),
                rs.getString("username"),
                rs.getString("phone"),
                rs.getString("company_name"),
                rs.getString("company_type"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("contact_email"),
                rs.getString("company_status"),
                rs.getString("account_status"),
                rs.getString("company_status"),
                rs.getString("account_status"),
                timestampToString(rs.getTimestamp("submitted_at")),
                timestampToString(rs.getTimestamp("reviewed_at")),
                nullableLong(rs, "reviewer_user_id"),
                rs.getString("review_comment"),
                List.of()
            ),
            normalizedStatus,
            normalizedStatus,
            normalizedStatus,
            normalizedKeyword,
            normalizedKeyword,
            normalizedKeyword,
            normalizedKeyword,
            normalizedKeyword,
            pageSize,
            offset
        );
        List<RegistrationReviewResponse> withQualifications = new ArrayList<>();
        for (RegistrationReviewResponse registration : registrations) {
            withQualifications.add(new RegistrationReviewResponse(
                registration.id(),
                registration.userId(),
                registration.companyId(),
                registration.username(),
                registration.phone(),
                registration.companyName(),
                registration.companyType(),
                registration.contactName(),
                registration.contactPhone(),
                registration.contactEmail(),
                registration.status(),
                registration.accountStatus(),
                registration.companyStatus(),
                registration.profileStatus(),
                registration.submittedAt(),
                registration.reviewedAt(),
                registration.reviewerUserId(),
                registration.reviewReason(),
                supplierServiceTypesForCompany(registration.companyId()),
                qualificationsForCompany(registration.companyId())
            ));
        }
        return withQualifications;
    }

    public void log(Long operatorUserId, String operationType, String targetType, String targetId, String path, String detail) {
        jdbcTemplate.update(
            """
            INSERT INTO operation_log
              (operator_user_id, operation_type, target_type, target_id, request_path, detail)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            operatorUserId,
            operationType,
            targetType,
            targetId,
            path,
            detail
        );
    }

    private Map<Long, List<String>> roleCodesByUserId() {
        Map<Long, List<String>> rolesByUserId = new HashMap<>();
        jdbcTemplate.query(
            """
            SELECT user_role.user_id, role.role_code
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id
            ORDER BY role.id
            """,
            rs -> {
                rolesByUserId.computeIfAbsent(rs.getLong("user_id"), ignored -> new ArrayList<>())
                    .add(rs.getString("role_code"));
            }
        );
        return rolesByUserId;
    }

    private List<MenuResponse> filterMenusForCompany(List<MenuResponse> menus, CompanyResponse company) {
        if (!"SUPPLIER".equals(company.companyType()) || company.supplierServiceTypes().isEmpty()) {
            return menus;
        }
        boolean material = company.supplierServiceTypes().contains("MATERIAL");
        boolean food = company.supplierServiceTypes().contains("FOOD");
        return menus.stream()
            .filter(menu -> {
                boolean materialMenu = "MATERIAL_PROCUREMENT_GROUP".equals(menu.menuCode())
                    || "MATERIAL_PROCUREMENT_GROUP".equals(menu.parentCode());
                boolean foodMenu = "FOOD_PROCUREMENT_GROUP".equals(menu.menuCode())
                    || "FOOD_PROCUREMENT_GROUP".equals(menu.parentCode());
                return (!materialMenu || material) && (!foodMenu || food);
            })
            .toList();
    }

    private String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String timestampToString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toString();
    }

    private Long nullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    public List<MenuResponse> buildTree(List<MenuResponse> flatMenus) {
        Map<String, MutableMenu> byCode = new LinkedHashMap<>();
        List<MutableMenu> roots = new ArrayList<>();
        for (MenuResponse menu : flatMenus) {
            byCode.put(menu.menuCode(), new MutableMenu(menu));
        }
        for (MutableMenu menu : byCode.values()) {
            String parentCode = menu.menu.parentCode();
            if (parentCode == null || parentCode.isBlank()) {
                roots.add(menu);
                continue;
            }
            MutableMenu parent = byCode.get(parentCode);
            if (parent == null) {
                roots.add(menu);
            } else {
                parent.children.add(menu);
            }
        }
        return roots.stream()
            .sorted(MutableMenu.BY_SORT_ORDER)
            .filter(MutableMenu::isRenderable)
            .map(MutableMenu::toResponse)
            .toList();
    }

    private static final class MutableMenu {
        private static final Comparator<MutableMenu> BY_SORT_ORDER = Comparator
            .comparingInt((MutableMenu mutableMenu) -> mutableMenu.menu.sortOrder())
            .thenComparing(mutableMenu -> mutableMenu.menu.menuCode());

        private final MenuResponse menu;
        private final List<MutableMenu> children = new ArrayList<>();

        private MutableMenu(MenuResponse menu) {
            this.menu = menu;
        }

        private MenuResponse toResponse() {
            return new MenuResponse(
                menu.menuCode(),
                menu.menuName(),
                menu.routePath(),
                menu.icon(),
                menu.parentCode(),
                menu.sortOrder(),
                menu.requiredPermission(),
                menu.visibleRoles(),
                children.stream()
                    .sorted(BY_SORT_ORDER)
                    .filter(MutableMenu::isRenderable)
                    .map(MutableMenu::toResponse)
                    .toList()
            );
        }

        private boolean isRenderable() {
            return menu.routePath() != null
                && !menu.routePath().isBlank()
                || children.stream().anyMatch(MutableMenu::isRenderable);
        }
    }

    public List<AdminMenuTreeResponse> buildAdminMenuTree(List<AdminMenuResponse> flatMenus) {
        Map<String, MutableAdminMenu> byCode = new LinkedHashMap<>();
        List<MutableAdminMenu> roots = new ArrayList<>();
        for (AdminMenuResponse menu : flatMenus) {
            byCode.put(menu.menuCode(), new MutableAdminMenu(menu));
        }
        for (MutableAdminMenu menu : byCode.values()) {
            String parentCode = menu.menu.parentCode();
            if (parentCode == null || parentCode.isBlank()) {
                roots.add(menu);
                continue;
            }
            MutableAdminMenu parent = byCode.get(parentCode);
            if (parent == null) {
                roots.add(menu);
            } else {
                parent.children.add(menu);
            }
        }
        return roots.stream()
            .sorted(MutableAdminMenu.BY_SORT_ORDER)
            .map(MutableAdminMenu::toResponse)
            .toList();
    }

    private static final class MutableAdminMenu {
        private static final Comparator<MutableAdminMenu> BY_SORT_ORDER = Comparator
            .comparingInt((MutableAdminMenu mutableMenu) -> mutableMenu.menu.sortOrder())
            .thenComparing(mutableMenu -> mutableMenu.menu.menuCode());

        private final AdminMenuResponse menu;
        private final List<MutableAdminMenu> children = new ArrayList<>();

        private MutableAdminMenu(AdminMenuResponse menu) {
            this.menu = menu;
        }

        private AdminMenuTreeResponse toResponse() {
            return new AdminMenuTreeResponse(
                menu.menuId(),
                menu.menuCode(),
                menu.menuName(),
                menu.routePath(),
                menu.icon(),
                menu.parentCode(),
                menu.sortOrder(),
                menu.requiredPermission(),
                menu.visibleRoles(),
                menu.enabled(),
                children.stream()
                    .sorted(BY_SORT_ORDER)
                    .map(MutableAdminMenu::toResponse)
                    .toList()
            );
        }
    }
}
