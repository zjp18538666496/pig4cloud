import service from "@/utils/request.js";

/**
 * 登录
 * @param data
 * @return {*}
 */
export function getRoleLists(data) {
    return service({
        url: "/role/getRoleLists",
        method: "post",
        data,
    });
}

export function delRole(data) {
    return service({
        url: "/role/delRole",
        method: "post",
        data,
    });
}
