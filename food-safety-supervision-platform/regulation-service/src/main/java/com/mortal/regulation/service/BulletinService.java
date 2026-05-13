package com.mortal.regulation.service;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.BulletinSaveDTO;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;

public interface BulletinService {

    PageResult<BulletinVO> listAdmin(Long userId, String keyword, String category, String status, int page, int size);

    BulletinDetailVO getAdminDetail(Long userId, Long bulletinId);

    BulletinDetailVO create(Long userId, String username, BulletinSaveDTO dto);

    BulletinDetailVO update(Long userId, String username, Long bulletinId, BulletinSaveDTO dto);

    BulletinDetailVO publish(Long userId, String username, Long bulletinId);

    BulletinDetailVO offline(Long userId, String username, Long bulletinId);

    PageResult<BulletinVO> listPublic(String keyword, String category, int page, int size);

    BulletinDetailVO getPublicDetail(Long bulletinId);
}
