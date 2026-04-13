ALTER TABLE public_bulletin
  CHANGE COLUMN summary category VARCHAR(64) NOT NULL COMMENT '公告类别';
