package com.zswy.shipsupply.procurement.materials;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class MaterialImpaCategories {

    private static final Map<String, String> NAMES = names();

    private MaterialImpaCategories() {
    }

    static Set<String> codes() {
        return NAMES.keySet();
    }

    static Map<String, String> namesByCode() {
        return NAMES;
    }

    static String name(String code) {
        return NAMES.getOrDefault(code, "待确认大类");
    }

    private static Map<String, String> names() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("11", "船员后勤、娱乐用品");
        names.put("15", "亚麻布类");
        names.put("17", "厨房用品");
        names.put("19", "服装");
        names.put("21", "绳索、钢丝绳");
        names.put("23", "甲板消耗品");
        names.put("25", "船舶油漆");
        names.put("27", "涂装用具类");
        names.put("31", "安全防护用品");
        names.put("33", "救生救难用具、消防器类");
        names.put("35", "管件、连接器");
        names.put("37", "航海仪器");
        names.put("39", "医疗、卫生用品");
        names.put("45", "石油制品类");
        names.put("47", "文具类");
        names.put("49", "五金类");
        names.put("51", "刷子、垫子");
        names.put("53", "盥洗设备");
        names.put("55", "清洁用品、化学品");
        names.put("59", "风动、电动工具");
        names.put("61", "一般作业工具类");
        names.put("63", "切削工具");
        names.put("65", "测量工具");
        names.put("67", "金属板材、棒材");
        names.put("69", "螺钉、螺帽类");
        names.put("71", "管材");
        names.put("73", "管路附件");
        names.put("75", "阀门、旋塞");
        names.put("77", "轴承");
        names.put("79", "电器设备");
        names.put("81", "密封用品");
        names.put("85", "焊接设备");
        names.put("87", "机械及其他设备");
        return Map.copyOf(names);
    }
}
