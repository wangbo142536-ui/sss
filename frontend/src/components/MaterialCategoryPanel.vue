<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { getMaterialStandardCategories } from "@/services/standardLibraryService";
import type { StandardCategoryNode } from "@/types/standardLibrary";

const categories = ref<StandardCategoryNode[]>([]);
const activeCode = ref("");
const keyword = ref("");
const loading = ref(true);

const filteredCategories = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return categories.value;

  const matchedCategories: StandardCategoryNode[] = [];

  categories.value.forEach((category) => {
    const children = category.children ?? [];
    const categoryMatched = [category.code, category.nameCn, category.nameEn]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query));
    const matchedChildren = children.filter((child) =>
      [child.code, child.nameCn, child.nameEn].filter(Boolean).some((value) => String(value).toLowerCase().includes(query))
    );

    if (categoryMatched || matchedChildren.length) {
      matchedCategories.push({
        ...category,
        children: categoryMatched ? children : matchedChildren
      });
    }
  });

  return matchedCategories;
});

const activeCategory = computed(() => {
  return filteredCategories.value.find((category) => category.code === activeCode.value) ?? filteredCategories.value[0];
});

const activeChildren = computed(() => activeCategory.value?.children ?? []);

const totalSecondLevelCount = computed(() => {
  return categories.value.reduce((total, category) => total + (category.children?.length ?? 0), 0);
});

onMounted(async () => {
  loading.value = true;
  const result = await getMaterialStandardCategories();
  categories.value = result.categories;
  activeCode.value = result.categories[0]?.code ?? "";
  loading.value = false;
});
</script>

<template>
  <section class="material-tree" aria-label="物料标准库前两级分类">
    <div class="tree-head">
      <div>
        <h2>物料标准库</h2>
        <p>按 IMPA 标准库展示一级目录和二级码段，供清单匹配和物料筛选使用。</p>
      </div>
    </div>

    <div class="tree-toolbar">
      <label class="tree-search">
        <span>搜索分类</span>
        <input v-model="keyword" type="search" placeholder="输入一级名称、二级码段或中文名" />
      </label>
      <div class="tree-stats">
        <span>{{ categories.length }} 个一级目录</span>
        <span>{{ totalSecondLevelCount }} 个二级码段</span>
      </div>
    </div>

    <div v-if="loading" class="tree-loading" aria-live="polite">
      <span></span>
      <span></span>
      <span></span>
    </div>

    <div v-else-if="filteredCategories.length" class="tree-grid">
      <nav class="tree-parents" aria-label="一级分类">
        <button
          v-for="category in filteredCategories"
          :key="category.code"
          type="button"
          :class="{ active: category.code === activeCategory?.code }"
          @click="activeCode = category.code"
          @mouseenter="activeCode = category.code"
        >
          <span class="node-code">{{ category.code }}</span>
          <span class="node-main">
            <b>{{ category.nameCn }}</b>
            <small>{{ category.itemCount.toLocaleString("zh-CN") }} 项标准物料</small>
          </span>
          <span class="node-count">{{ category.children?.length ?? 0 }}</span>
        </button>
      </nav>

      <div class="tree-children" aria-label="二级分类">
        <div class="children-head" v-if="activeCategory">
          <div>
            <h3>{{ activeCategory.code }} · {{ activeCategory.nameCn }}</h3>
            <p>{{ activeCategory.nameEn || "IMPA 一级目录" }}</p>
          </div>
          <span>{{ activeChildren.length }} 个二级码段</span>
        </div>

        <div class="children-list" v-if="activeChildren.length">
          <article v-for="child in activeChildren" :key="child.code" class="tree-child">
            <div>
              <b>{{ child.code }}</b>
              <span>{{ child.nameCn }}</span>
            </div>
            <strong>{{ child.itemCount.toLocaleString("zh-CN") }} 项</strong>
          </article>
        </div>

        <div v-else class="tree-empty">
          当前一级目录暂无二级码段，等待后端按 IMPA 编码聚合后返回。
        </div>
      </div>
    </div>

    <div v-else class="tree-empty">
      没有找到匹配的分类，请换一个关键词。
    </div>
  </section>
</template>
