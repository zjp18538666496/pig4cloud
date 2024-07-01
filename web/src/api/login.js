import service from "@/utils/request.js";

export function login(data) {
  return service({
    url: "/api/user/login",
    method: "post",
    data,
  });
}
