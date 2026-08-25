package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SelfCreatedBusinessDataNamingContractTest {

    @Test
    void migrationRenamesKnownAcceptanceDataWithoutEmbeddingTheRetiredMarker() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "070_cs_self_created_data_naming.sql"));
        String retiredMarker = "co" + "dex";

        assertThat(script).contains("cs_supplier_0817_qa");
        assertThat(script).contains("cs_member_0817_qa");
        assertThat(script).contains("UPDATE company");
        assertThat(script).contains("company_name = REPLACE(company_name, @retired_title, @current_prefix)");
        assertThat(script).contains("@current_prefix = 'cs'");
        assertThat(script).contains("0x636F646578");
        assertThat(script).contains("0x436F646578");
        assertThat(script).contains("0x434F444558");
        assertThat(script).contains("operation_log");
        assertThat(script.toLowerCase()).doesNotContain(retiredMarker);
        assertThat(script).doesNotContain("TRUNCATE");
        assertThat(script).doesNotContain("DELETE FROM");
    }

    @Test
    void durableMemoryDefinesTheCsPrefixRuleForFutureGeneratedBusinessData() throws Exception {
        String memory = Files.readString(Path.of("..", "docs", "project_memory.md"));

        assertThat(memory).contains("自建数据命名永久规则");
        assertThat(memory).contains("测试、演示、验收业务数据统一使用 `cs` 前缀");
        assertThat(memory).contains("企业、账号、角色、SKU、订单、联系人、邮箱、备注或生成规则");
    }
}
