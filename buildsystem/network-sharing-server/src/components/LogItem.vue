<template>
  <td class="align-middle">{{ logfile.date }}</td>
  <td class="text-center">
    <a :href="href" class="">
      <button class="btn btn-lg log-download" aria-label="download" type="button">
        <i class="fa fa-download"></i>
      </button>
    </a>
  </td>
</template>

<script>
import { API_CONFIG } from '@/config';
export default {
  props: {
    logfile: Object
  },
  computed: {
    href: function () {
      return `${API_CONFIG.DOWNLOAD_LOGFILE}` + this.logfile.path
    },
    iclass: function () {
      return `fa fa-` + this.type
    },
  },
  mounted: function () {
  }
}
</script>

<style lang="scss">
@import '../scss/app.scss';

.log-download {
  color: $primary-color;
}
</style>