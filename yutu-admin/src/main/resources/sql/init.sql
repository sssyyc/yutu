CREATE DATABASE IF NOT EXISTS yutu_travel DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE yutu_travel;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS complaint_flow;
DROP TABLE IF EXISTS complaint_order;
DROP TABLE IF EXISTS tour_contract_signature;
DROP TABLE IF EXISTS tour_contract_appendix;
DROP TABLE IF EXISTS tour_contract;
DROP TABLE IF EXISTS contract_template;
DROP TABLE IF EXISTS pay_record;
DROP TABLE IF EXISTS tour_order_traveler;
DROP TABLE IF EXISTS tour_order;
DROP TABLE IF EXISTS tour_review;
DROP TABLE IF EXISTS user_favorite;
DROP TABLE IF EXISTS user_traveler;
DROP TABLE IF EXISTS tour_departure_date;
DROP TABLE IF EXISTS tour_route_schedule;
DROP TABLE IF EXISTS tour_route_tag;
DROP TABLE IF EXISTS tour_route;
DROP TABLE IF EXISTS tour_tag;
DROP TABLE IF EXISTS tour_category;
DROP TABLE IF EXISTS merchant_shop;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_banner;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  avatar VARCHAR(255),
  role_type TINYINT NOT NULL COMMENT '1用户 2商家 3管理员',
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(64) NOT NULL,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_name VARCHAR(100) NOT NULL,
  path VARCHAR(128),
  component VARCHAR(128),
  perms VARCHAR(128),
  menu_type TINYINT NOT NULL DEFAULT 1 COMMENT '1菜单 2按钮',
  icon VARCHAR(64),
  sort_num INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_shop (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  shop_name VARCHAR(100) NOT NULL,
  contact_name VARCHAR(64),
  contact_phone VARCHAR(32),
  description VARCHAR(255),
  license_no VARCHAR(64),
  license_image VARCHAR(255),
  id_card_front_image VARCHAR(255),
  id_card_back_image VARCHAR(255),
  audit_status TINYINT NOT NULL DEFAULT 1 COMMENT '0待审 1通过 2拒绝',
  audit_remark VARCHAR(255),
  audit_time DATETIME,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  category_name VARCHAR(64) NOT NULL,
  sort_num INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag_name VARCHAR(64) NOT NULL,
  tag_type VARCHAR(32),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  route_name VARCHAR(100) NOT NULL,
  cover_image VARCHAR(255),
  summary VARCHAR(255),
  detail_content TEXT,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  stock INT NOT NULL DEFAULT 0,
  score DECIMAL(3,1) NOT NULL DEFAULT 5.0,
  audit_status TINYINT NOT NULL DEFAULT 1 COMMENT '0待审 1通过 2拒绝',
  audit_remark VARCHAR(255),
  publish_status TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_route_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_route_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  day_no INT NOT NULL,
  title VARCHAR(100),
  content TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_departure_date (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  depart_date DATE NOT NULL,
  remain_count INT NOT NULL DEFAULT 0,
  sale_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  audit_status TINYINT NOT NULL DEFAULT 1 COMMENT '0待审核 1通过 2驳回',
  audit_remark VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  target_type VARCHAR(32) NOT NULL DEFAULT 'ROUTE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_traveler (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  traveler_name VARCHAR(64) NOT NULL,
  id_card VARCHAR(32) NOT NULL,
  phone VARCHAR(32),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  route_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 5,
  content VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  route_id BIGINT NOT NULL,
  depart_date_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  traveler_count INT NOT NULL DEFAULT 1,
  order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAY',
  pay_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  contract_status VARCHAR(32) NOT NULL DEFAULT 'NOT_GENERATED',
  source VARCHAR(32) DEFAULT 'WEB',
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_order_traveler (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  traveler_name VARCHAR(64) NOT NULL,
  id_card VARCHAR(32) NOT NULL,
  phone VARCHAR(32),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pay_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  pay_no VARCHAR(64) NOT NULL,
  pay_type VARCHAR(32) NOT NULL DEFAULT 'MOCK',
  pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  pay_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
  pay_time DATETIME,
  callback_content TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE contract_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(100) NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  template_content TEXT,
  download_count BIGINT NOT NULL DEFAULT 0,
  use_count BIGINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255),
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_contract (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  contract_title VARCHAR(150),
  contract_content TEXT,
  sign_status VARCHAR(32) NOT NULL DEFAULT 'UNSIGNED',
  sign_time DATETIME,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_contract_signature (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  signer_name VARCHAR(64) NOT NULL,
  signature_image LONGTEXT NOT NULL,
  sign_time DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_contract_signature_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_contract_appendix (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  appendix_title VARCHAR(100) NOT NULL,
  appendix_content TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE complaint_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  complaint_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  contract_id BIGINT,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  complaint_type VARCHAR(32) NOT NULL DEFAULT 'SERVICE',
  title VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_ACCEPT',
  result_type VARCHAR(32),
  result_content VARCHAR(255),
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE complaint_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  complaint_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  operator_role VARCHAR(32) NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  action_content VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  link_url VARCHAR(255),
  sort_num INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_dict_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(64) NOT NULL,
  dict_label VARCHAR(64) NOT NULL,
  dict_value VARCHAR(64) NOT NULL,
  sort_num INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role (id, role_name, role_code, status) VALUES
(1, '普通用户', 'USER', 1),
(2, '商家', 'MERCHANT', 1),
(3, '管理员', 'ADMIN', 1);

INSERT INTO sys_user (id, username, password, nickname, phone, role_type, status, deleted) VALUES
(1, 'user01', '123456', '演示用户', '13800000001', 1, 1, 0),
(2, 'merchant01', '123456', '演示商家', '13800000002', 2, 1, 0),
(3, 'admin01', '123456', '平台管理员', '13800000003', 3, 1, 0);

INSERT INTO merchant_shop (id, user_id, shop_name, contact_name, contact_phone, description, license_no, license_image, id_card_front_image, id_card_back_image, audit_status, audit_remark, audit_time, status, deleted) VALUES
(1, 2, '豫途官方体验店', '商家张三', '13800000002', '河南本地精品线路服务商', 'HN-LIC-2026-0001', 'https://picsum.photos/seed/license1/640/420', 'https://picsum.photos/seed/idfront1/640/420', 'https://picsum.photos/seed/idback1/640/420', 1, '初始化示例商户，已审核通过', NOW(), 1, 0);

INSERT INTO tour_category (id, parent_id, category_name, sort_num, status, deleted) VALUES
(1, 0, '周边游', 1, 1, 0),
(2, 0, '跟团游', 2, 1, 0),
(3, 0, '研学游', 3, 1, 0);

INSERT INTO tour_tag (id, tag_name, tag_type, status, deleted) VALUES
(1, '家庭亲子', 'STYLE', 1, 0),
(2, '文化古都', 'THEME', 1, 0),
(3, '自然风光', 'THEME', 1, 0),
(4, '周末可出发', 'TIME', 1, 0);

INSERT INTO tour_route (id, merchant_id, category_id, route_name, cover_image, summary, detail_content, price, stock, score, audit_status, audit_remark, publish_status, status, deleted) VALUES
(1, 1, 1, '郑州-洛阳两日文化游', 'https://picsum.photos/600/300?1', '龙门石窟+白马寺+应天门', '含住宿与门票，适合家庭及学生群体', 699.00, 100, 4.9, 1, NULL, 1, 1, 0),
(2, 1, 1, '嵩山少林一日游', 'https://picsum.photos/600/300?2', '少林寺+塔林+武术表演', '含往返交通与导游服务', 299.00, 120, 4.8, 1, NULL, 1, 1, 0);

INSERT INTO tour_route_tag (route_id, tag_id) VALUES
(1, 1), (1, 2), (1, 4), (2, 3), (2, 4);

INSERT INTO tour_route_schedule (route_id, day_no, title, content) VALUES
(1, 1, '郑州出发-洛阳古城', '上午集合出发，下午游览白马寺与古城夜景'),
(1, 2, '龙门石窟返程', '上午游览龙门石窟，下午返程'),
(2, 1, '登嵩山访少林', '全天游览少林寺与塔林');

INSERT INTO tour_departure_date (id, route_id, depart_date, remain_count, sale_price, status, audit_status, audit_remark) VALUES
(1, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 30, 699.00, 1, 1, NULL),
(2, 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 25, 699.00, 1, 1, NULL),
(3, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 50, 299.00, 1, 1, NULL);

INSERT INTO contract_template (id, template_name, template_code, version_no, template_content, status, remark, deleted) VALUES
(1, '标准旅游合同模板', 'STANDARD_TRAVEL', 'v1.0', '甲方（游客）与乙方（商家）就旅游服务达成如下协议：......', 1, '系统默认模板', 0);

INSERT INTO sys_banner (title, image_url, link_url, sort_num, status) VALUES
('春季特惠', 'https://picsum.photos/1200/300?travel1', '/route/detail/1', 1, 1),
('亲子周边游', 'https://picsum.photos/1200/300?travel2', '/route/detail/2', 2, 1);

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_num, status) VALUES
('ORDER_STATUS', '待支付', 'PENDING_PAY', 1, 1),
('ORDER_STATUS', '已支付', 'PAID', 2, 1),
('ORDER_STATUS', '待出行', 'PENDING_TRAVEL', 3, 1),
('ORDER_STATUS', '已完成', 'COMPLETED', 4, 1),
('ORDER_STATUS', '已取消', 'CANCELLED', 5, 1),
('ORDER_STATUS', '退款中', 'REFUNDING', 6, 1),
('ORDER_STATUS', '已退款', 'REFUNDED', 7, 1);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort_num, status) VALUES
(1, 0, '用户首页', '/', 'views/user/Home.vue', 'home:view', 1, 'House', 1, 1),
(2, 0, '路线列表', '/route/list', 'views/user/RouteList.vue', 'route:list', 1, 'MapLocation', 2, 1),
(3, 0, '收藏', '/favorite', 'views/user/Favorite.vue', 'favorite:list', 1, 'Star', 3, 1),
(4, 0, '订单', '/order/list', 'views/user/OrderList.vue', 'order:list', 1, 'Tickets', 4, 1),
(5, 0, '合同', '/contract/list', 'views/user/ContractList.vue', 'contract:list', 1, 'Document', 5, 1),
(6, 0, '投诉', '/complaint/list', 'views/user/ComplaintList.vue', 'complaint:list', 1, 'Warning', 6, 1),
(7, 0, '个人中心', '/user/profile', 'views/user/UserProfile.vue', 'user:profile:view', 1, 'User', 7, 1),

(20, 0, '商家工作台', '/merchant', 'views/merchant/MerchantHome.vue', 'merchant:dashboard:view', 1, 'OfficeBuilding', 20, 1),
(21, 20, '店铺信息', '/merchant/shop', 'views/merchant/Shop.vue', 'merchant:shop:view', 1, 'Shop', 21, 1),
(22, 20, '路线管理', '/merchant/route/list', 'views/merchant/RouteList.vue', 'merchant:route:list', 1, 'List', 22, 1),
(23, 20, '订单管理', '/merchant/order/list', 'views/merchant/OrderList.vue', 'merchant:order:list', 1, 'Tickets', 23, 1),
(24, 20, '客户管理', '/merchant/customer/list', 'views/merchant/CustomerList.vue', 'merchant:customer:list', 1, 'UserFilled', 24, 1),
(25, 20, '合同管理', '/merchant/contract/list', 'views/merchant/ContractList.vue', 'merchant:contract:list', 1, 'Document', 25, 1),
(26, 20, '投诉处理', '/merchant/complaint/list', 'views/merchant/ComplaintList.vue', 'merchant:complaint:list', 1, 'WarningFilled', 26, 1),
(27, 20, '经营统计', '/merchant/statistics', 'views/merchant/Statistics.vue', 'merchant:stats:view', 1, 'DataAnalysis', 27, 1),

(40, 0, '管理后台', '/admin', 'views/admin/AdminHome.vue', 'admin:dashboard:view', 1, 'Setting', 40, 1),
(41, 40, '数据大屏', '/admin/dashboard', 'views/admin/Dashboard.vue', 'admin:dashboard:view', 1, 'DataLine', 41, 1),
(42, 40, '用户管理', '/admin/user/list', 'views/admin/UserList.vue', 'admin:user:list', 1, 'User', 42, 1),
(43, 40, '商家管理', '/admin/merchant/list', 'views/admin/MerchantList.vue', 'admin:merchant:list', 1, 'Shop', 43, 1),
(44, 40, '分类管理', '/admin/category/list', 'views/admin/CategoryList.vue', 'admin:category:list', 1, 'CollectionTag', 44, 1),
(45, 40, '标签管理', '/admin/tag/list', 'views/admin/TagList.vue', 'admin:tag:list', 1, 'Collection', 45, 1),
(46, 40, '路线审核', '/admin/route/audit', 'views/admin/RouteAudit.vue', 'admin:route:list', 1, 'MapLocation', 46, 1),
(47, 40, '合同模板', '/admin/contract-template/list', 'views/admin/ContractTemplateList.vue', 'admin:contract-template:list', 1, 'Document', 47, 1),
(48, 40, '订单管理', '/admin/order/list', 'views/admin/OrderList.vue', 'admin:order:list', 1, 'Tickets', 48, 1),
(49, 40, '支付记录', '/admin/pay/list', 'views/admin/PayList.vue', 'admin:pay:list', 1, 'Money', 49, 1),
(50, 40, '投诉处理', '/admin/complaint/list', 'views/admin/ComplaintList.vue', 'admin:complaint:list', 1, 'Warning', 50, 1),
(51, 40, '统计分析', '/admin/statistics', 'views/admin/Statistics.vue', 'admin:statistics:view', 1, 'PieChart', 51, 1),
(52, 40, '轮播图管理', '/admin/banner/list', 'views/admin/BannerList.vue', 'admin:banner:list', 1, 'Picture', 52, 1),

(100, 0, '用户收藏新增', NULL, NULL, 'favorite:add', 2, NULL, 100, 1),
(101, 0, '用户收藏删除', NULL, NULL, 'favorite:delete', 2, NULL, 101, 1),
(102, 0, '用户下单', NULL, NULL, 'order:create', 2, NULL, 102, 1),
(103, 0, '用户支付', NULL, NULL, 'order:pay', 2, NULL, 103, 1),
(104, 0, '用户取消订单', NULL, NULL, 'order:cancel', 2, NULL, 104, 1),
(105, 0, '用户退款', NULL, NULL, 'order:refund', 2, NULL, 105, 1),
(106, 0, '用户签署合同', NULL, NULL, 'contract:sign', 2, NULL, 106, 1),
(107, 0, '用户投诉发起', NULL, NULL, 'complaint:create', 2, NULL, 107, 1),
(108, 0, '用户出行人管理', NULL, NULL, 'traveler:manage', 2, NULL, 108, 1),
(109, 0, '用户资料更新', NULL, NULL, 'user:profile:update', 2, NULL, 109, 1),
(110, 0, '用户密码更新', NULL, NULL, 'user:password:update', 2, NULL, 110, 1),

(120, 0, '商家店铺更新', NULL, NULL, 'merchant:shop:update', 2, NULL, 120, 1),
(121, 0, '商家路线新增', NULL, NULL, 'merchant:route:create', 2, NULL, 121, 1),
(122, 0, '商家路线编辑', NULL, NULL, 'merchant:route:update', 2, NULL, 122, 1),
(123, 0, '商家路线上下架', NULL, NULL, 'merchant:route:publish', 2, NULL, 123, 1),
(124, 0, '商家合同补充', NULL, NULL, 'merchant:contract:appendix', 2, NULL, 124, 1),
(125, 0, '商家投诉回复', NULL, NULL, 'merchant:complaint:reply', 2, NULL, 125, 1),

(140, 0, '管理员用户状态', NULL, NULL, 'admin:user:status', 2, NULL, 140, 1),
(141, 0, '管理员用户删除', NULL, NULL, 'admin:user:delete', 2, NULL, 141, 1),
(142, 0, '管理员商家审核', NULL, NULL, 'admin:merchant:audit', 2, NULL, 142, 1),
(143, 0, '管理员分类维护', NULL, NULL, 'admin:category:manage', 2, NULL, 143, 1),
(144, 0, '管理员标签维护', NULL, NULL, 'admin:tag:manage', 2, NULL, 144, 1),
(145, 0, '管理员路线审核动作', NULL, NULL, 'admin:route:audit', 2, NULL, 145, 1),
(146, 0, '管理员模板维护', NULL, NULL, 'admin:contract-template:manage', 2, NULL, 146, 1),
(147, 0, '管理员订单处理', NULL, NULL, 'admin:order:handle', 2, NULL, 147, 1),
(148, 0, '管理员投诉处理', NULL, NULL, 'admin:complaint:handle', 2, NULL, 148, 1),
(149, 0, '管理员轮播图维护', NULL, NULL, 'admin:banner:manage', 2, NULL, 149, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 1 AND 7 OR id BETWEEN 100 AND 110;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id BETWEEN 20 AND 27 OR id BETWEEN 120 AND 125;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE id BETWEEN 40 AND 52 OR id BETWEEN 140 AND 149;

SET FOREIGN_KEY_CHECKS = 1;
SET NAMES utf8mb4;
