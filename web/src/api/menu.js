import service from "@/utils/request.js";

/**
 * 登录
 * @param data
 * @return {*}
 */
export function getMenuLists(data) {
    return service({
        url: "/menu/getMenuLists",
        method: "post",
        data,
    });
}

export function delMenu(data) {
    return service({
        url: "/menu/delMenu",
        method: "post",
        data,
    });
}
export function updateMenu(data) {
    return service({
        url: "/menu/updateMenu",
        method: "post",
        data,
    });
}

export function createMenu(data) {
    return service({
        url: "/menu/createMenu",
        method: "post",
        data,
    });
}