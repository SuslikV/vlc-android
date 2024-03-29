import { defineStore } from 'pinia'

/**
 * Manages the state of the player
 */
export const usePlayerStore = defineStore('player', {

  state: () => ({
    playing: false,
    nowPlaying: Object,
    playqueueData: Object,
    playqueueShowing: false,
    playQueueEdit: false,
    responsivePlayerShowing: false,
  }),
  getters: {
  },
  actions: {
    togglePlayQueueEdit() {
      this.playQueueEdit = !this.playQueueEdit
    },
  },
})