import service from "@/utils/request.js";

export function getUser(id) {
    return service({
        url: "/user/id/" + id,
        method: "get"
    });
}

export function createUser(data) {
    return service({
        url: "/user/createUser",
        method: "POST",
        data
    });
}

export function updateUser(data) {
    return service({
        url: "/user/updateUser",
        method: "POST",
        data
    });
}
