<template>
    <div v-if="loaded && this.videos.length !== 0" class="container">
        <div v-if="this.appStore.displayType[this.$route.name]" class="row gx-3 gy-3 media-list">
            <template v-for="video in videos" :key="video.id">
                <MediaListItem :media="video" :downloadable="true" :mediaType="'video'" />
            </template>
        </div>
        <div v-else class="row gx-3 gy-3 media-content">
            <div class="col-md-3 col-sm-4 col-6" v-for="video in videos" :key="video.id">
                <MediaCardItem :media="video" :downloadable="true" :mediaType="'video'" />
            </div>
        </div>
    </div>
    <div v-else-if="loaded" class="empty-view-container">
        <EmptyView :message="getEmptyText()" />
    </div>
</template>

<script>

import http from '../plugins/auth'
import { vlcApi } from '../plugins/api.js'
import { useAppStore } from '../stores/AppStore'
import { mapStores } from 'pinia'
import EmptyView from '../components/EmptyView.vue'
import MediaCardItem from '../components/MediaCardItem.vue'
import MediaListItem from '../components/MediaListItem.vue'

export default {
    components: {
        EmptyView,
        MediaCardItem,
        MediaListItem,
    },
    computed: {
        ...mapStores(useAppStore)
    },
    data() {
        return {
            videos: [],
            loaded: false,
            forbidden: false,
        }
    },
    methods: {
        fetchVideos() {
            let component = this
            component.appStore.loading = true
            http.get(vlcApi.videoList)
                .catch(function (error) {
                    if (error.response !== undefined && error.response.status == 403) {
                        component.forbidden = true;
                    }
                })
                .then((response) => {
                    this.loaded = true;
                    if (response) {
                        component.forbidden = false;
                        this.videos = response.data
                    }
                    component.appStore.loading = false
                });
        },
        getEmptyText() {
            if (this.forbidden) return this.$t('FORBIDDEN')
            return this.$t('NO_MEDIA')
        }
    },
    mounted: function () {
        this.appStore.$subscribe((mutation, state) => {
            console.log(`Something changed in the app store: ${mutation} -> ${state}`)
            if (mutation.events.key == "needRefresh" && mutation.events.newValue === true) {
                this.fetchVideos();
                this.appStore.needRefresh = false
            }
        })

    },
    created: function () {
        this.fetchVideos();
    }
}
</script>


<style lang="scss">
@import '../scss/colors.scss';

.ratio>.resolution {
    position: absolute;
    top: 8px;
    left: 8px;
    width: auto;
    height: auto;
    color: #fff;
    background: rgba(0, 0, 0, 0.6);
    padding: 4px 6px;
    border-radius: 2px;
    font-size: 0.6rem;
}

.ratio>.played {
    position: absolute;
    top: 8px;
    right: 8px;
    left: auto;
    width: 24px;
    height: 24px;
    padding: 4px;
    color: #fff;
    background: rgba(0, 0, 0, 0.6);
    border-radius: 2px;
    font-size: 0.6rem;
}

.card-progress-container {
    height: 14px;
    position: absolute;
    bottom: 0;
    top: auto;
    border-radius: 6px;
    overflow: hidden;
}
.card-progress {
    height: 4px;
    background-color: $primary-color;
    position: absolute;
    bottom: 0;
}
.card-progress.full {
    width: 100%;
    background-color: $light-grey-transparent;
}
</style>
