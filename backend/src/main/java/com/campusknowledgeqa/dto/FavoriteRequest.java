package com.campusknowledgeqa.dto;

/** 收藏设置请求：favorite 为空时按当前状态取反（切换） */
public record FavoriteRequest(Boolean favorite) {
}
