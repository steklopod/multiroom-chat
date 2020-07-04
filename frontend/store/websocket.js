import SockJS from "sockjs-client";
import Stomp from "webstomp-client";

export const state = () => ({
  websocketUrl: "http://localhost:8080/ws",
  connected: false
});

export const getters = {
  connected: state => state.connected
};

export const mutations = {
  setConnected(state, status) {
    state.connected = status;
  }
};

export const actions = {
  connect({state, commit}) {
    if (state.connected) return;
    this.socket = new SockJS(state.websocketUrl);
    this.stompClient = Stomp.over(this.socket);

    // comment the line below if you want to see debug messages
    this.stompClient.debug = msg => {
      console.log("MESSAGE: " + msg)
    };

    this.stompClient.connect(
      {}, () => {
        commit("setConnected", true);

        this.stompClient.subscribe("/app/chat/roomList", tick => {
          console.log("subscribe {/app/chat/roomList}, tick: ", tick)

          const roomList = JSON.parse(tick.body);
          console.log("subscribe {/app/chat/roomList}, roomList: ", roomList)

          commit("main/initRoom", roomList, {root: true});
        });

        // subscribe new rooms
        this.stompClient.subscribe("/chat/newRoom", tick => {
          console.log("subscribe {chat/newRoom}, tick: ", tick)

          const room = JSON.parse(tick.body);
          console.log("subscribe {chat/newRoom}, room: ", room)

          commit("main/addRoom", room, {root: true});
        });

      },
      error => {
        console.error(error);
        commit("setConnected", false);
      }
    );
  },

  subscribeRoomList() {

  }
};
