<template>
  <div class="login_container">
    <div class="background">
      <img src='/public/img/background/login_bg.cbfed30c.svg'>
    </div>
    <div class="login-box">
      <div class="title">PIGX ADMIN</div>
      <div class="from">
        <component :is="currentComponent" @zc="zc" @dl="dl"></component>
      </div>
    </div>
  </div>
</template>
<script setup>
import { shallowRef } from 'vue'
import Login from "@/views/login/components/Login.vue";
import Register from "@/views/login/components/Register.vue";

const currentComponent = shallowRef(Login)
const dl = () => {
  currentComponent.value = Register
}

const zc = () => {
  currentComponent.value = Login
}
</script>
<style scoped>

.login_container {
  display: flex;
  width: 100vw;
  height: 100vh;
  background-color: #f8f8f8;
  background-image: url('/public/img/background/bg.7b14eacd.png');
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  background-size: cover;

  .background {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding-right: 130px;
    width: 50vw;

    img {
      width: 500px;
    }
  }

  .login-box {
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding-left: 100px;
    width: 50vw;

    .title {
      text-align: center;
      padding: 12px 0;
      width: 400px;
      font-weight: 600;
      font-size: 30px;

    }

    .from {
      width: 400px;
      padding: 48px;
      background-color: #fff;
      border-radius: 20px;
    }

  }
}
</style>
