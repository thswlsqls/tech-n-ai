package com.tech.n.ai.batch.source.common.utils;


public class CodeVal {

    /**
     * [SATURN] 물류 BO 배송완료 상태 업데이트 배치 잡
     * */
    public final static String SATURN_LOGISTICS_DELIVERED = "saturn.logistics.delivered.job";

    /**
     * [SATURN] 물류 BO 배송완료 상태 업데이트 병렬처리 배치 잡
     * */
    public final static String SATURN_LOGISTICS_DELIVERED_PARTITIONING = "saturn.logistics.delivered.partitioning.job";

    /**
     * [SATURN] 물류 BO 출고 송장 배송상태 알림 배치 잡
     * */
    public final static String SATURN_LOGISTICS_DELIVERY_STATUS = "saturn.logistics.delivery.status.job";

    /**
     * [SATURN] 물류 BO 출고 송장 배송상태 알림 배치 잡
     * */
    public final static String SATURN_LOGISTICS_DELIVERY_STATUS_PARTITIONING = "saturn.logistics.delivery.status.partitioning.job";

    /**
     * [SATURN] 물류 BO 유효하지 않은 출고 송장 알림 배치 잡
     * */
    public final static String SATURN_LOGISTICS_INVALID_INVOICE = "saturn.logistics.invalid.invoice.job";

    /**
     * [SATURN] 물류 BO 유효하지 않은 출고 송장 알림 배치 잡
     * */
    public final static String SATURN_LOGISTICS_INVALID_INVOICE_PARTITIONING = "saturn.logistics.invalid.invoice.partitioning.job";

    /**
     * [SATURN] 물류 BO 자동 송장등록 배치 잡
     * */
    public final static String SATURN_LOGISTICS_INVOICE = "saturn.logistics.invoice.job";

    /**
     * [SATURN] 물류 BO 자동 송장등록 파티셔닝 배치 잡
     * */
    public final static String SATURN_LOGISTICS_INVOICE_PARTITIONING = "saturn.logistics.invoice.partitioning.job";

    /**
     * [SATURN] 물류 BO 자동 출고지시 배치 잡 | 8/13/16
     * */
    public final static String SATURN_LOGISTICS_SHIP_OUT = "saturn.logistics.ship.out.job";

    /**
     * [SATURN] 물류 BO 자동 출고지시 병렬처리 배치 잡 | 8/13/16
     * */
    public final static String SATURN_LOGISTICS_SHIP_OUT_PARTITIONING = "saturn.logistics.ship.out.partitioning.job";

    /**
     * [SATURN] 공제조합 등록 요청 실패 건 재요청 배치 잡
     * */
    public final static String SATURN_DEDUCT_REISSUE = "saturn.deduct.reissue.job";

    /**
     * [SATURN] 공제조합 등록취소 요청 실패 건 재요청 배치 잡
     * */
    public final static String SATURN_DEDUCT_RECANCEL = "saturn.deduct.recancel.job";

    /**
     * [SATURN] 구매확정 배치 잡 [(주문일 + 14일) -> (배치실행일 - 15일)]
     * */
    public final static String SATURN_ORDER_CONFIRM = "saturn.order.confirm.job";

    /**
     * [SATURN] 구매확정(탈퇴회원) 배치 잡 [(주문일 + 14일) -> (배치실행일 - 15일)]
     * */
    public final static String SATURN_ORDER_CONFIRM_WITHDRAWAL_MEMBER = "saturn.order.confirm.withdrawal.member.job";

    /**
     * [SATURN] 가상계좌 만료 잡 ( 주문 당일 23:59:00 )
     */
    public final static String SATURN_EXPIRE_VIRTUAL_ACCOUNT = "saturn.order.expire.virtual.account.job";

    /**
     * [SATURN] 토스 현금영수증 대사 배치 ( 배치 실행일 - 1일 00:00:00 ~ 23:59:59 )
     */
    public final static String SATURN_ORDER_CASH_RECEIPT_MATCH = "saturn.order.cash.receipt.match.job";

    /**
     * [SATURN] 번퓸 접수 취소 잡 (반품접수일 + 3일 초과)
     */
    public final static String SATURN_RETURN_REQUEST_CANCEL = "saturn.order.return.request.cancel.job";

    /**
     * [SATURN] 번퓸 접수 취소 예정 UMS (반품접수일 + 2알)
     */
    public final static String SATURN_RETURN_REQUEST_CANCEL_EXPECT = "saturn.order.return.request.cancel.expect.job";

    /**
     * [SATURN] 글로벌 주문 이벤트 재처리
     */
    public final static String SATURN_FAILURE_ORDER_EVENT_RETRY = "saturn.failure.order.event.retry.job";


    /**
     * [SATURN] GRIT 데이터 이전시 BP/SP 계산 배치 잡
     */
    public final static String SATURN_GRIT_ORDER_BP_SP_CALCULATOR_JOB = "saturn.grit.order.bp.sp.calculator.job";

    /**
     * [SATURN] 정기구매 품절(예정) 알림 배치 잡
     */
    public final static String SATURN_AUTOSHIP_CREATE_DISCONTINUED_ALARM_JOB = "saturn.autoship.create.discontinued.alarm.job";

    /**
     * [SATURN] 정기구매 자동 결제 예정 알림 배치 잡
     */
    public final static String SATURN_AUTOSHIP_CREATE_UPCOMING_PAYMENT_ALARM_JOB = "saturn.autoship.create.upcoming.payment.alarm.job";

    /**
     * [SATURN] 정기구매 환불동의 필요 안내 배치 잡
     */
    public final static String SATURN_AUTOSHIP_CREATE_RETURN_REQUEST_NOT_ALLOWED_ALARM_JOB = "saturn.autoship.create.return.request.not.allowed.alarm.job";


    /**
     * [SATURN] 정기구매 자동 결제 미완료 안내 배치 잡
     */
    public final static String SATURN_AUTOSHIP_CREATE_UNPAID_ALARM_JOB = "saturn.autoship.create.unpaid.alarm.job";


    /**
     * [SATURN] 장바구니 90일 이후 삭제 처리 (일반, 정기구매 장바구니)
     */
    public final static String SATURN_DELETE_EXPIRED_CART_JOB = "saturn.order.expired.autoship.cart.job";

    public final static String SATURN_PRODUCT_BIOME_PROMOTION_STOCK_SEND_ALERTS_JOB = "saturn.product.biome.promotion.stock.send.alerts.job";

    /**
     * [SATURN] 재고 관제 알림 배치 잡
     */
    public final static String SATURN_STOCK_NOTI = "saturn.stock.noti.job";

    /**
     * [SATURN] 정기구매 예상판매 데이터 수집 잡
     */
    public final static String SATURN_AUTOSHIP_EXPECTED_JOB = "saturn.autoship.expected.job";


    // JOB PARAMETER
    public final static String PARAMETER = ".parameter";

    // STEP
    public final static String STEP_1 = ".step.1";
    public final static String STEP_2 = ".step.2";
    public final static String STEP_3 = ".step.3";
    public final static String STEP_4 = ".step.4";


    // TASKLET
    public final static String TASKLET = ".tasklet";


    // CHUNK SIZE
    public final static int CHUNK_SIZE_2 = 2;
    public final static int CHUNK_SIZE_5 = 5;
    public final static int CHUNK_SIZE_10 = 10;
    public final static int CHUNK_SIZE_50 = 50;
    public final static int CHUNK_SIZE_100 = 100;
    public final static int CHUNK_SIZE_300 = 300;
    public final static int CHUNK_SIZE_1000 = 1000;
    public final static int CHUNK_SIZE_2000 = 2000;

    public static final String AUTOSHIP_AUTO_PAYMENT_JOB = "saturn.autoship.auto.payment.job";

    public static final String SPLIT_PAYMENT_CANCEL_JOB = "saturn.order.split.payment.cancel.job";

    public static final String AUTOSHIP_WITHDRAWAL_JOB = "saturn.autoship.withdrawal.job";

    public static final String AUTOSHIP_WITHDRAWAL_OVERSKIP_JOB = "saturn.autoship.withdrawal.overskip.job";

    public static final String AUTOSHIP_WITHDRAWAL_NOTI_SEND_JOB = "saturn.autoship.withdrawal.noti.send.job";

    public final static String ITEM_READER = ".item.reader";
    public final static String ITEM_PROCESSOR = ".item.processor";
    public final static String ITEM_WRITER = ".item.writer";

    // Partitioning
    public final static int GRID_SIZE_4 = 4;
    public final static String MANAGER = ".manager";
    public final static String WORKER = ".worker";
    public final static String TASK_POOL = ".task.pool";
    public final static String PARTITION_HANDLER  = ".partition.handler";
    public final static String PARTITIONER = ".partitioner";

    // Retry
    public final static String BACKOFF_POLICY = ".backoff.policy";

}

