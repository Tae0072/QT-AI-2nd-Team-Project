package com.qtai.domain.qt.api.admin;

import com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse;
import com.qtai.domain.qt.api.admin.dto.ListAdminQtPassagesQuery;

public interface ListAdminQtPassagesUseCase {

    AdminQtPassageListResponse list(ListAdminQtPassagesQuery query);
}
