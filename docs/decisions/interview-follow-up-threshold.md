# Quyết định — Ngưỡng follow-up (BR-05)

> Trạng thái: ⚠️ giả định — cần xác nhận với SRS gốc nếu có biểu thức tường minh khác.

## Bối cảnh

BR-05 yêu cầu AI **đề xuất** follow-up và **Java quyết định** có hỏi không. SRS ghi
“initial follow-up score >=70%, maximum 2 consecutive follow-ups” nhưng cách diễn
đải “>=70%” mơ hồ khi đọc cùng `follow_up_config.trigger_threshold = 70` và
`score_thresholds.good = 70`.

## Quyết định (giả định)

Triển khai theo nghĩa hợp lý nhất với tên cấu hình `trigger_threshold`:

> Hỏi follow-up khi `total_score% < trigger_threshold (70)` **VÀ** số follow-up
liên tiếp < `max_consecutive (2)`.

Nghĩa là: đáp án **chưa đạt mức “good” (>=70%)** thì mới追问 để làm rõ — đáp án
xuất sắc (>=70%) không cần follow-up. Tối đa 2 follow-up liên tiếp cho một câu gốc.

## Lý do

- Khớp với tên `trigger_threshold` (ngưỡng dưới đó thì “kích hoạt” follow-up).
- Khớp với `score_thresholds.good = 70` (đạt good → không cần hỏi thêm).
- Tránh hỏi follow-up khi ứng viên đã trả lời tốt.

## Vị trí code

`application/interview/InterviewService.submitAnswer`:
```java
if (scorePct < cfg.followUpTriggerThreshold() && consec < cfg.followUpMaxConsecutive() && followUpQuestion != null)
```

## Đảo ngược / rủi ro

Nếu SRS thực sự muốn follow-up khi `score% >= 70` (tức hỏi thêm để đào sâu đáp án
tốt), chỉ cần đổi dấu so sánh. Là tham số cấu hình nên không cần đổi code ngoài
dòng so sánh. Cần xác nhận với nghiệp vụ trước khi đẩy production.
