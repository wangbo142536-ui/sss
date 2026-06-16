package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthRepositoryMenuTreeTest {

    @Test
    void buildTreeOrdersSiblingMenusBySortOrderThenMenuCode() {
        AuthRepository repository = new AuthRepository(org.mockito.Mockito.mock(JdbcTemplate.class));
        List<MenuResponse> menus = repository.buildTree(List.of(
            menu("SETTLEMENT", "SETTLEMENT", null, 90),
            parent("BASIC_MANAGEMENT", "Basic management", 110),
            menu("MENU_MANAGEMENT", "Menu management", "BASIC_MANAGEMENT", 40),
            menu("COMPANY_MEMBERS", "Company members", null, 100),
            menu("CREW_SERVICE", "Crew service", null, 30),
            parent("FOOD_PROCUREMENT_GROUP", "Food procurement", 20),
            menu("FOOD_ORDERS", "Purchase management", "FOOD_PROCUREMENT_GROUP", 40),
            menu("FOOD_INQUIRIES", "Inquiry management", "FOOD_PROCUREMENT_GROUP", 10),
            menu("PROCUREMENT_FOOD", "Food procurement entry", "FOOD_PROCUREMENT_GROUP", 0),
            menu("FOOD_COMPARISON", "Comparison management", "FOOD_PROCUREMENT_GROUP", 30),
            menu("FOOD_QUOTES", "Quote management", "FOOD_PROCUREMENT_GROUP", 20),
            parent("MATERIAL_PROCUREMENT_GROUP", "Material procurement", 10),
            menu("ORDERS", "Purchase management", "MATERIAL_PROCUREMENT_GROUP", 40),
            menu("QUOTES", "Quote management", "MATERIAL_PROCUREMENT_GROUP", 20),
            menu("INQUIRIES", "Inquiry management", "MATERIAL_PROCUREMENT_GROUP", 10),
            menu("PROCUREMENT_MATERIALS", "Material procurement entry", "MATERIAL_PROCUREMENT_GROUP", 0),
            menu("COMPARISON", "Comparison management", "MATERIAL_PROCUREMENT_GROUP", 30),
            menu("DASHBOARD", "DASHBOARD", null, 0)
        ));

        assertThat(menus).extracting(MenuResponse::menuCode)
            .containsExactly(
                "DASHBOARD",
                "MATERIAL_PROCUREMENT_GROUP",
                "FOOD_PROCUREMENT_GROUP",
                "CREW_SERVICE",
                "SETTLEMENT",
                "COMPANY_MEMBERS",
                "BASIC_MANAGEMENT"
            );
        assertThat(menus.get(1).children()).extracting(MenuResponse::menuCode)
            .containsExactly("PROCUREMENT_MATERIALS", "INQUIRIES", "QUOTES", "COMPARISON", "ORDERS");
        assertThat(menus.get(2).children()).extracting(MenuResponse::menuCode)
            .containsExactly("PROCUREMENT_FOOD", "FOOD_INQUIRIES", "FOOD_QUOTES", "FOOD_COMPARISON", "FOOD_ORDERS");
        assertThat(menus.get(6).children()).extracting(MenuResponse::menuCode)
            .containsExactly("MENU_MANAGEMENT");
    }

    @Test
    void buildTreeKeepsRouteLessParentWhenUserHasChildMenuPermission() {
        AuthRepository repository = new AuthRepository(org.mockito.Mockito.mock(JdbcTemplate.class));
        List<MenuResponse> menus = repository.buildTree(List.of(
            parent("BASIC_MANAGEMENT", "Basic management", 30),
            menu("STANDARD_LIBRARY_IMPA", "IMPA library", "BASIC_MANAGEMENT", 0),
            parent("EMPTY_GROUP", "Empty group", 40)
        ));

        assertThat(menus).extracting(MenuResponse::menuCode)
            .containsExactly("BASIC_MANAGEMENT");
        assertThat(menus.get(0).children()).extracting(MenuResponse::menuCode)
            .containsExactly("STANDARD_LIBRARY_IMPA");
    }

    @Test
    void buildTreeDoesNotLoopWhenMenusContainParentCycle() {
        AuthRepository repository = new AuthRepository(org.mockito.Mockito.mock(JdbcTemplate.class));
        List<MenuResponse> menus = repository.buildTree(List.of(
            menu("CYCLE_A", "A", "CYCLE_B", 0),
            menu("CYCLE_B", "B", "CYCLE_A", 10),
            menu("DASHBOARD", "Dashboard", null, 20)
        ));

        assertThat(menus).extracting(MenuResponse::menuCode)
            .containsExactly("DASHBOARD");
    }

    private MenuResponse menu(String menuCode, String menuName, String parentCode, int sortOrder) {
        return new MenuResponse(
            menuCode,
            menuName,
            "/" + menuCode.toLowerCase(),
            null,
            parentCode,
            sortOrder,
            menuCode + "_VIEW",
            "SHIP_AGENT",
            List.of()
        );
    }

    private MenuResponse parent(String menuCode, String menuName, int sortOrder) {
        return new MenuResponse(
            menuCode,
            menuName,
            null,
            "Settings",
            null,
            sortOrder,
            null,
            "SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN",
            List.of()
        );
    }
}
