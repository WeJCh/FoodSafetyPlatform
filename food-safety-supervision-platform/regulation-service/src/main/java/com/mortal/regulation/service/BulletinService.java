package com.mortal.regulation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.BulletinSaveDTO;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;

public interface BulletinService {

    PageResult<BulletinVO> listAdmin(Long userId, String keyword, String category, String status, int page, int size);

    BulletinDetailVO getAdminDetail(Long userId, Long bulletinId);

    BulletinDetailVO create(Long userId, BulletinSaveDTO dto);

    BulletinDetailVO update(Long userId, Long bulletinId, BulletinSaveDTO dto);

    BulletinDetailVO publish(Long userId, Long bulletinId);

    BulletinDetailVO offline(Long userId, Long bulletinId);

    PageResult<BulletinVO> listPublic(String keyword, String category, int page, int size);

    BulletinDetailVO getPublicDetail(Long bulletinId);
}
