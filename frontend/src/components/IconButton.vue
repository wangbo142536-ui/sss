<script setup lang="ts">
withDefaults(
  defineProps<{
    icon:
      | "Search"
      | "Plus"
      | "RefreshCw"
      | "Upload"
      | "Download"
      | "Save"
      | "Send"
      | "Eye"
      | "Pencil"
      | "Trash2"
      | "Ban"
      | "Check"
      | "ArrowUp"
      | "ArrowDown"
      | "ChevronLeft"
      | "ChevronRight"
      | "Home"
      | "X"
      | "Maximize2"
      | "Minimize2"
      | "MoreHorizontal"
      | "Image";
    label: string;
    glyph?: string;
    variant?: "primary" | "secondary" | "danger" | "plain";
    type?: "button" | "submit";
    disabled?: boolean;
    loading?: boolean;
  }>(),
  {
    variant: "secondary",
    type: "button",
    disabled: false,
    loading: false
  }
);

defineEmits<{
  click: [event: MouseEvent];
}>();
</script>

<template>
  <button
    :type="type"
    :class="['ui-icon-button', `ui-icon-button--${variant}`, { 'is-loading': loading }]"
    :aria-label="label"
    :title="label"
    :data-tooltip="label"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
    <span v-else-if="glyph" class="ui-icon-button__glyph" aria-hidden="true">{{ glyph }}</span>
    <svg v-else viewBox="0 0 24 24" aria-hidden="true">
      <path v-if="icon === 'Search'" d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
      <path v-else-if="icon === 'Plus'" d="M12 5v14M5 12h14" />
      <path v-else-if="icon === 'RefreshCw'" d="M20 11a8 8 0 0 0-14.6-4.5L4 8m0 0V3m0 5h5M4 13a8 8 0 0 0 14.6 4.5L20 16m0 0v5m0-5h-5" />
      <path v-else-if="icon === 'Upload'" d="M12 15V4m0 0 4 4m-4-4-4 4M4 15v4h16v-4" />
      <path v-else-if="icon === 'Download'" d="M12 4v11m0 0 4-4m-4 4-4-4M4 17v3h16v-3" />
      <path v-else-if="icon === 'Save'" d="M5 4h12l2 2v14H5V4Zm3 0v6h8V4M8 20v-6h8v6" />
      <path v-else-if="icon === 'Send'" d="M21 3 10 14M21 3l-7 18-4-7-7-4 18-7Z" />
      <path v-else-if="icon === 'Eye'" d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4Z" />
      <path v-else-if="icon === 'Pencil'" d="m4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Z M13.5 6.5l4 4" />
      <path v-else-if="icon === 'Trash2'" d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" />
      <path v-else-if="icon === 'Ban'" d="M5.6 5.6a9 9 0 1 0 12.8 12.8A9 9 0 0 0 5.6 5.6Zm0 0 12.8 12.8" />
      <path v-else-if="icon === 'Check'" d="m5 12 4 4L19 6" />
      <path v-else-if="icon === 'ArrowUp'" d="M12 19V5m0 0-6 6m6-6 6 6" />
      <path v-else-if="icon === 'ArrowDown'" d="M12 5v14m0 0-6-6m6 6 6-6" />
      <path v-else-if="icon === 'ChevronLeft'" d="m15 18-6-6 6-6" />
      <path v-else-if="icon === 'ChevronRight'" d="m9 6 6 6-6 6" />
      <path v-else-if="icon === 'Home'" d="M3 11.5 12 4l9 7.5M5.5 10.5V20h13v-9.5M9.5 20v-6h5v6" />
      <path v-else-if="icon === 'X'" d="M6 6l12 12M18 6 6 18" />
      <path v-else-if="icon === 'Maximize2'" d="M8 3H3v5M3 3l7 7M16 3h5v5M21 3l-7 7M8 21H3v-5M3 21l7-7M16 21h5v-5M21 21l-7-7" />
      <path v-else-if="icon === 'Minimize2'" d="M10 3v7H3M3 10l7-7M14 3v7h7M21 10l-7-7M10 21v-7H3M3 14l7 7M14 21v-7h7M21 14l-7 7" />
      <path v-else-if="icon === 'MoreHorizontal'" d="M5 12h.01M12 12h.01M19 12h.01" />
      <path v-else-if="icon === 'Image'" d="M4 5h16v14H4V5Zm3 10 3-3 2 2 3-4 3 5M8 8h.01" />
    </svg>
  </button>
</template>
