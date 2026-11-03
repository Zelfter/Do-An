# SRS — extracted text
> Reference copy of supplied document; original DOCX is authoritative.

# Đặc tả yêu cầu phần mềm (SRS): Ứng dụng web ôn Java và phỏng vấn mô phỏng theo JD

Oct 16, 2026 · @Zenos

Tài liệu này đặc tả yêu cầu, phạm vi MVP và thiết kế hệ thống của ứng dụng web giúp ôn kiến thức Java và luyện phỏng vấn bám theo JD, làm cơ sở trực tiếp cho thiết kế và phát triển.

## 1. Giới thiệu

1.1 Mục đích và phạm vi

Tài liệu dành cho chủ dự án (đồng thời là BA, Product Owner, kiến trúc sư và lập trình viên) và cho giảng viên hướng dẫn hoặc hội đồng đánh giá.

Sản phẩm làm những việc sau: người dùng dán JD; hệ thống phân tích JD thành các chủ đề kiến thức; phỏng vấn mô phỏng bằng chat có tính giờ, với câu hỏi lấy từ ngân hàng câu hỏi bằng RAG; AI chấm từng câu rồi hỏi sâu hoặc chuyển chủ đề; khi phiên kết thúc, người dùng nhận báo cáo, danh sách kiến thức yếu và nội dung ôn tập tương ứng. Có phân hệ Admin để quản lý nội dung và người dùng.

Ngoài phạm vi phiên bản này: phỏng vấn bằng giọng nói, nộp và phân tích CV, nội dung và giao diện tiếng Anh, vai trò Mentor hoặc Recruiter, thanh toán, ứng dụng mobile native, tự huấn luyện mô hình riêng.

1.2 Bối cảnh dự án

Dự án cá nhân, quy mô và độ phức tạp tương đương đồ án tốt nghiệp đại học ✅.

Ngôn ngữ lập trình dự kiến: Java (ưu tiên backend) và Python (AI service) ✅.

1.3 Quy ước trình bày

Ký hiệu | Ý nghĩa

✅ | Yêu cầu hoặc quyết định do chủ dự án nêu hoặc xác nhận trực tiếp

💡 | Đề xuất của nhóm phân tích, đã được chấp thuận cùng phạm vi MVP

⚠️ | Giả định, ước tính hoặc điểm chưa xác nhận (tổng hợp ở mục 13)


Mã định danh: FR (chức năng), NFR (phi chức năng), BR (quy tắc nghiệp vụ), UC (use case), US (user story), AC (tiêu chí nghiệm thu). Mức ưu tiên theo MoSCoW: Must, Should, Could, Won't.

1.4 Tình trạng xác nhận

Giai đoạn 1 (Idea Discovery), 2 (Requirements Analysis) và 3 (MVP Definition) đã được chủ dự án xác nhận.

Thiết kế ở Giai đoạn 4 (mục 10) được xem là chấp thuận khi chủ dự án yêu cầu chuyển sang Giai đoạn 5; vẫn có thể điều chỉnh.

Số liệu về hosting và API miễn phí được tra cứu tháng 10/2026 và có thể thay đổi, cần kiểm tra lại khi triển khai.

1.5 Thuật ngữ

Thuật ngữ | Giải thích

JD | Job Description, mô tả công việc do người dùng cung cấp

LLM | Mô hình ngôn ngữ lớn, dùng qua API gói miễn phí

RAG | Retrieval-Augmented Generation: truy xuất dữ liệu liên quan rồi đưa vào ngữ cảnh cho LLM

Embedding | Vector biểu diễn nghĩa của văn bản, dùng để tìm kiếm theo độ tương đồng

pgvector | Phần mở rộng của PostgreSQL để lưu và tìm kiếm vector

Chủ đề (topic) | Phạm vi kiến thức chuyên môn, ví dụ OOP, Collections, Spring Boot, Database

Phiên (session) | Một buổi phỏng vấn của một người dùng

Lượt (turn) | Một cặp câu hỏi và câu trả lời trong phiên

Follow-up | Câu hỏi đào sâu do AI sinh từ câu trả lời vừa rồi

Pool câu hỏi | Danh sách câu ứng viên cho từng chủ đề, tính một lần khi tạo phiên

Rubric | Bộ tiêu chí chấm điểm một câu trả lời

Cold start | Độ trễ khi một dịch vụ miễn phí đã ngủ phải khởi động lại

Prompt injection | Nội dung đầu vào cố ý chỉ dẫn AI làm sai quy tắc


## 2. Mô tả tổng quan

2.1 Vấn đề cần giải quyết

Kiến thức ôn phỏng vấn rời rạc, người học không biết cần ôn gì cho vị trí cụ thể ⚠️.

Thiếu môi trường luyện phản xạ: biết đáp án nhưng bí khi bị hỏi dồn, hỏi sâu hoặc bị giới hạn thời gian ✅.

Chuẩn bị không bám JD: mỗi JD yêu cầu một stack khác nhau ✅.

Không có phản hồi định lượng: tự học không biết mình yếu ở đâu và yếu đến mức nào ✅.

Giá trị cốt lõi là vòng lặp khép kín: phỏng vấn, phát hiện điểm yếu, học đúng phần yếu, phỏng vấn lại. Mọi quyết định thiết kế nên bảo vệ vòng lặp này.

2.2 Actors

Actor | Loại | Mô tả | Nguồn

Ứng viên (Candidate) | Chính | Dán JD, làm phỏng vấn, xem báo cáo, học phần yếu | ✅

Quản trị viên (Admin) | Chính | Quản lý chủ đề, câu hỏi, tài liệu, người dùng, cấu hình điểm; bắt buộc có trong MVP | ✅

AI Interviewer | Hệ thống nội bộ | Chọn câu hỏi bằng RAG, chấm điểm, sinh follow-up | ✅

Dịch vụ LLM và Embedding | Hệ thống ngoài | API gói miễn phí hoặc mô hình mã nguồn mở | ✅

Dịch vụ Speech (STT, TTS) | Hệ thống ngoài | Chỉ ở giai đoạn nâng cao, ngoài MVP | ✅


Nhóm người dùng mục tiêu (sinh viên năm cuối, fresher, junior Java) là giả định chưa được xác nhận ⚠️.

2.3 Luồng nghiệp vụ cốt lõi ✅

Ứng viên cung cấp JD (không cần CV).

Hệ thống phân tích JD, ánh xạ về các chủ đề kiến thức, ứng viên xác nhận.

Hệ thống phỏng vấn bằng chat, dùng câu hỏi có sẵn trong CSDL truy xuất theo RAG.

Sau mỗi câu trả lời, AI đánh giá mức đạt. Nếu đạt tốt, AI dùng khả năng sinh văn bản để đặt câu hỏi đào sâu trong phạm vi JD. Nếu không đạt hoặc đã hỏi xong, AI chuyển sang chủ đề kiến thức khác.

Điểm và nhận xét từng câu không hiển thị trong lúc chat.

Khi phiên kết thúc (ứng viên chủ động, hết giờ hoặc hết câu hỏi), hệ thống hiển thị báo cáo cùng danh sách kiến thức yếu và nội dung ôn tập.

2.4 Ràng buộc và quyết định đã xác nhận ✅

Chỉ dùng API, LLM mã nguồn mở hoặc dịch vụ miễn phí (ví dụ Google Gemini, Groq), không tự host LLM cục bộ.

Máy phát triển chỉ có 4GB RAM, không có GPU rời.

Bản demo cuối deploy lên hosting miễn phí (ví dụ Render, Railway, Vercel, Koyeb), nên kiến trúc phải tiết kiệm bộ nhớ và dễ triển khai.

Kiến trúc tách service: một Java Backend (nghiệp vụ chính, người dùng, lưu kết quả) và một Python AI Service (RAG, giao tiếp LLM API, chunking tài liệu).

Frontend dùng React hoặc Vue, giao tiếp với Backend qua REST API.

MVP chỉ có phỏng vấn bằng văn bản (chat). Voice là tính năng nâng cao, làm sau khi bản chat ổn định.

Ngôn ngữ MVP là tiếng Việt, thiết kế phải mở rộng được sang tiếng Anh.

Admin nằm trong MVP, không dùng script thủ công.

Không cần CV; chỉ cần JD để tạo phiên.

Các chỉ số nghiệp vụ tạm chốt, tinh chỉnh khi test: ngưỡng hỏi sâu 70%, tối đa 2 follow-up, trọng số rubric 50/30/10/10, ngưỡng kiến thức yếu 50%.

2.5 Giả định và phụ thuộc ⚠️

JD nhập vào có thể bằng tiếng Việt hoặc tiếng Anh; phỏng vấn xuất ra tiếng Việt.

Hạn mức gói miễn phí của Gemini và Groq đủ cho demo (khoảng 15 lệnh gọi LLM mỗi phiên) và có thể thay đổi bất cứ lúc nào.

Khoảng 8 chủ đề với khoảng 15 câu hỏi mỗi chủ đề (tổng khoảng 120 câu, tiếng Việt, có đáp án chuẩn và ý chính) là mức dữ liệu tối thiểu để demo.

Nền tảng hosting miễn phí giữ nguyên các giới hạn đã tra cứu (mục 10.6).

## 3. Yêu cầu chức năng (FR)

Sau các lần cập nhật có 40 FR đang hiệu lực (28 Must, 6 Should, 5 Could, 1 Won't) và 3 FR đã bị loại (FR-03, FR-05, FR-08). Cột Nguồn dùng ✅ cho yêu cầu do chủ dự án nêu và 💡 cho đề xuất đã được chấp thuận.

3.1 Tài khoản

ID | Yêu cầu | Ưu tiên | Nguồn

FR-01 | Đăng ký, đăng nhập, đăng xuất bằng email và mật khẩu | Must | 💡

FR-02 | Xem và chỉnh sửa hồ sơ cá nhân | Should | 💡


3.2 Đầu vào JD

ID | Yêu cầu | Ưu tiên | Nguồn

FR-04 | Nộp JD bằng cách dán văn bản | Must | ✅

FR-06 | Phân tích JD: trích xuất chủ đề, mức bắt buộc hoặc ưu tiên, level; ánh xạ về danh mục chủ đề của hệ thống | Must | ✅

FR-07 | Xem và chỉnh kết quả phân tích JD (bật hoặc tắt chủ đề, chọn level) trước khi bắt đầu | Must | ✅

FR-38 | Thông báo rõ các chủ đề trong JD mà hệ thống chưa hỗ trợ | Must | 💡

FR-42 | Tải file JD (PDF, DOCX) | Should | 💡


3.3 Cấu hình và điều phối phiên

ID | Yêu cầu | Ưu tiên | Nguồn

FR-09 | Tạo phiên phỏng vấn từ JD đã xác nhận | Must | ✅

FR-10 | Cấu hình phiên: số câu tối đa, thời lượng tổng | Must | ✅

FR-11 | Đồng hồ đếm toàn phiên, hiển thị thời gian còn lại; tạm dừng khi AI đang xử lý | Must | ✅

FR-12 | Giới hạn thời gian trả lời từng câu | Should | 💡

FR-13 | Tạm dừng và tiếp tục phiên | Could | 💡

FR-14 | Kết thúc phiên sớm theo yêu cầu ứng viên | Must | 💡


3.4 Phỏng vấn (lõi hệ thống)

ID | Yêu cầu | Ưu tiên | Nguồn

FR-15 | Truy xuất câu hỏi bằng RAG theo chủ đề JD | Must | ✅

FR-16 | Giao diện chat văn bản để AI hỏi và ứng viên trả lời | Must | ✅

FR-17 | Chấm từng câu trả lời theo rubric; kết quả được lưu, không hiển thị trong phiên | Must | ✅

FR-18 | Sinh câu hỏi follow-up khi câu trả lời đạt ngưỡng, trong phạm vi JD | Must | ✅

FR-19 | Chuyển sang câu hoặc chủ đề khác khi không đạt hoặc hết lượt follow-up | Must | ✅

FR-20 | Giới hạn số follow-up liên tiếp (tối đa 2 cho mỗi câu gốc) | Must | ✅

FR-21 | Bảo đảm follow-up không lệch phạm vi chủ đề của JD | Must | ✅

FR-22 | Không lặp lại câu hỏi đã hỏi trong cùng phiên | Must | 💡

FR-23 | Lưu toàn bộ transcript của phiên | Must | 💡

FR-24 | Xử lý AI lỗi hoặc quá chậm: retry, provider dự phòng, câu hỏi từ pool | Must | 💡

FR-43 | Tạo báo cáo bất đồng bộ với trạng thái GENERATING, READY hoặc PARTIAL | Must | 💡


3.5 Báo cáo và phản hồi sau phỏng vấn

ID | Yêu cầu | Ưu tiên | Nguồn

FR-25 | Báo cáo điểm tổng và điểm theo chủ đề | Must | ✅

FR-26 | Nhận xét từng câu (lý do chấm, ý chính còn thiếu), sinh khi tạo báo cáo | Must | 💡

FR-27 | Xác định chủ đề kiến thức yếu | Must | ✅

FR-28 | Từ chủ đề yếu mở được nội dung ôn tập tương ứng | Must | ✅


3.6 Lịch sử và Knowledge Hub

ID | Yêu cầu | Ưu tiên | Nguồn

FR-29 | Danh sách và chi tiết các phiên đã làm | Should | 💡

FR-30 | Biểu đồ tiến bộ giữa các phiên | Could | 💡

FR-31 | Duyệt và tìm kiếm Knowledge Hub độc lập theo chủ đề | Should | ✅

FR-40 | Luyện tập theo chủ đề, không cần JD | Could | 💡

FR-41 | Xuất báo cáo ra PDF | Could | 💡


3.7 Admin

ID | Yêu cầu | Ưu tiên | Nguồn

FR-32 | Quản lý ngân hàng câu hỏi (CRUD) kèm đáp án chuẩn và ý chính | Must | ✅

FR-33 | Quản lý tài liệu kiến thức và lập chỉ mục embedding cho RAG | Must | ✅

FR-34 | Quản lý người dùng (khóa, mở tài khoản) | Must | ✅

FR-37 | Quản lý danh mục chủ đề và từ khóa đồng nghĩa (alias) | Must | 💡

FR-39 | Nhập hàng loạt câu hỏi từ CSV hoặc JSON | Should | 💡

FR-35 | Thống kê sử dụng và lỗi AI | Could | 💡


3.8 Nâng cao và đã loại

ID | Yêu cầu | Trạng thái

FR-36 | Phỏng vấn bằng giọng nói (STT cho câu trả lời, TTS cho câu hỏi) | Won't trong phiên bản này ✅

FR-03 | Nộp CV | Đã loại theo quyết định của chủ dự án ✅

FR-05 | Trích xuất thông tin từ CV | Đã loại ✅

FR-08 | Phỏng vấn chỉ có CV | Đã loại ✅


3.9 Tổng hợp ưu tiên

Mức | Số FR | Ghi chú

Must | 28 | Bắt buộc cho MVP

Should | 6 | Làm ngay sau MVP nếu còn thời gian

Could | 5 | Nâng cao, làm sau

Won't | 1 | Voice, để phát triển sau đồ án


## 4. Yêu cầu phi chức năng (NFR)

Các mục tiêu về thời gian áp dụng khi hệ thống đã thức; thời gian khởi động nguội (cold start) được xử lý riêng ở NFR-31. Các con số đánh dấu ⚠️ là mục tiêu đề xuất, cần hiệu chỉnh khi đo thực tế.

4.1 Hiệu năng và thời gian phản hồi AI

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-01 | Hiển thị câu hỏi đầu tiên sau khi tạo phiên trong tối đa 10 giây | Must | ⚠️

NFR-02 | Từ lúc nộp câu trả lời đến khi có câu tiếp theo (chấm điểm và sinh câu): mục tiêu 15 giây, tối đa 30 giây | Must | ⚠️

NFR-03 | Hiển thị câu hỏi dạng streaming (chữ hiện dần); chỉ cần polling trạng thái ở MVP | Could | ⚠️

NFR-04 | Có chỉ báo AI đang xử lý; thời gian chờ AI không tính vào đồng hồ của ứng viên | Must | 💡

NFR-05 | Phân tích JD trong tối đa 30 giây | Must | ⚠️


4.2 Ràng buộc API và mô hình miễn phí

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-06 | Chỉ dùng LLM và embedding mã nguồn mở hoặc gói miễn phí; không phát sinh chi phí bắt buộc | Must | ✅

NFR-07 | Tầng truy cập AI trừu tượng hóa nhà cung cấp (provider-agnostic) để đổi mô hình mà không sửa nghiệp vụ | Must | 💡

NFR-08 | Xử lý rate limit và quota: retry có backoff, thông báo thân thiện | Must | 💡

NFR-09 | Giới hạn số phiên mỗi ngày mỗi người dùng và độ dài đầu vào để bảo vệ quota | Must | 💡

NFR-10 | Có fallback (provider dự phòng hoặc câu hỏi có sẵn) khi dịch vụ AI không khả dụng | Must | 💡

NFR-11 | Đầu ra AI theo định dạng JSON có cấu trúc và được validate (mô hình nhỏ dễ sai định dạng) | Must | 💡

NFR-12 | Toàn bộ hệ thống chạy trên hosting miễn phí, mỗi service trong giới hạn 512MB RAM | Must | ✅


4.3 Độ tin cậy và chất lượng AI

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-13 | Chấm nhất quán: chấm lại cùng một câu trả lời lệch không quá ±10% điểm | Must | ⚠️

NFR-14 | Kết quả chấm kèm lý do để kiểm chứng | Must | 💡

NFR-15 | Hạn chế ảo giác: đáp án chuẩn, ý chính và đoạn tài liệu truy xuất luôn được đưa vào ngữ cảnh chấm | Must | 💡

NFR-16 | Phiên đang chạy khôi phục được khi tải lại trang hoặc mất kết nối ngắn | Must | 💡


4.4 Bảo mật và quyền riêng tư

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-17 | Mật khẩu được băm (bcrypt hoặc argon2); xác thực bằng JWT | Must | 💡

NFR-18 | Cho phép người dùng xóa dữ liệu của mình | Should | 💡

NFR-19 | Phân quyền theo vai trò Candidate và Admin | Must | 💡

NFR-20 | Chống prompt injection từ JD và câu trả lời: nội dung người dùng chỉ là dữ liệu, điểm do Java tính và kiểm khoảng hợp lệ | Must | 💡

NFR-21 | Thông báo cho người dùng rằng JD và câu trả lời được gửi tới API bên thứ ba, có thể được dùng để huấn luyện mô hình | Should | 💡


4.5 Khả dụng và bảo trì

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-22 | Giao diện web responsive, ưu tiên desktop | Should | 💡

NFR-23 | Kiến trúc module hóa để thêm Voice sau mà không viết lại lõi phỏng vấn | Must | 💡

NFR-24 | Ghi log mỗi lần gọi AI (mục đích, nhà cung cấp, mô hình, thời gian, lỗi) | Must | 💡

NFR-25 | Phục vụ tối đa khoảng 5 phiên đồng thời (giới hạn bởi RPM của API miễn phí) | Must | ⚠️


4.6 Ngôn ngữ và cấu hình

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-26 | Tiếng Việt là ngôn ngữ chính: giao diện, câu hỏi, nhận xét và follow-up bằng tiếng Việt; thuật ngữ kỹ thuật giữ tiếng Anh | Must | ✅

NFR-27 | Thiết kế sẵn khả năng mở rộng tiếng Anh: không hard-code chuỗi và prompt; ngôn ngữ là thuộc tính của nội dung | Must | ✅

NFR-28 | LLM và embedding phải xử lý tốt tiếng Việt; có bộ kiểm thử nhỏ để chọn mô hình | Must | 💡

NFR-29 | Ngưỡng, trọng số và giới hạn follow-up nằm ở cấu hình, không hard-code | Must | 💡


4.7 Tài nguyên và môi trường

ID | Yêu cầu | Ưu tiên | Nguồn

NFR-30 | Mỗi service chạy được trong 512MB RAM và 0,1 vCPU: JVM tinh chỉnh heap, AI service không dùng thư viện ML nặng như torch | Must | 💡

NFR-31 | Chịu được cold start: giao diện báo đang khởi động, có gọi khởi động trước, tự thử lại; thời gian khởi động không tính vào NFR-01, NFR-02 và đồng hồ ứng viên | Must | 💡

NFR-32 | Ngân sách AI: tối đa khoảng 20 lệnh gọi LLM mỗi phiên, có bộ đếm quota (mở rộng NFR-09) | Must | 💡

NFR-33 | Phát triển được trên máy 4GB không GPU, không bắt buộc Docker; DB phát triển dùng nhánh Neon riêng | Must | 💡


## 5. Quy tắc nghiệp vụ (BR)

Logic khung (đạt thì hỏi sâu, không đạt thì chuyển câu) do chủ dự án nêu ✅. Các ngưỡng và trọng số được chốt tạm và sẽ tinh chỉnh khi test; chúng được lưu ở cấu hình (NFR-29).

5.1 Chấm điểm một câu trả lời

ID | Quy tắc | Nguồn

BR-01 | Rubric mỗi câu, mỗi tiêu chí chấm thang 0 đến 10 rồi quy ra phần trăm: Chính xác 50%, Đầy đủ 30%, Rõ ràng 10%, Ví dụ hoặc ứng dụng thực tế 10%. LLM chỉ trả điểm từng tiêu chí, điểm tổng do Java tính | ✅

BR-02 | Mỗi câu hỏi trong DB có đáp án chuẩn và danh sách ý chính; việc chấm dựa trên so khớp với dữ liệu này, không chỉ dựa vào cảm nhận của LLM | 💡

BR-03 | Phân loại điểm câu: từ 70% trở lên là Tốt; từ 40% đến dưới 70% là Trung bình; dưới 40% là Yếu | ✅


5.2 Hỏi sâu và chuyển câu

ID | Quy tắc | Nguồn

BR-04 | Chuyển chủ đề khi hết câu hỏi phù hợp của chủ đề, hoặc đạt giới hạn follow-up, hoặc trả lời yếu; theo thứ tự ưu tiên của JD | ✅

BR-05 | Chỉ hỏi follow-up khi đồng thời: điểm câu trước từ 70% trở lên; số follow-up liên tiếp nhỏ hơn 2; chủ đề nằm trong phạm vi JD; còn đủ thời gian và số câu | ✅

BR-06 | Follow-up phải bám vào ý trong câu trả lời vừa rồi, thuộc chủ đề từ JD, không trùng câu đã hỏi, độ khó không thấp hơn câu gốc | 💡

BR-07 | Phạm vi câu hỏi hoàn toàn theo JD; CV không được dùng | ✅

BR-08 | Độ khó khởi điểm lấy từ level trong JD (người dùng có thể chỉnh); tăng nếu liên tiếp Tốt, giảm nếu liên tiếp Yếu | 💡


5.3 Kết thúc phiên và tổng điểm

ID | Quy tắc | Nguồn

BR-09 | Phiên kết thúc khi: hết giờ, đủ số câu tối đa, hết chủ đề, hoặc ứng viên kết thúc sớm | ✅

BR-10 | Điểm chủ đề bằng trung bình điểm các câu của chủ đề; follow-up có trọng số bằng câu gốc | 💡

BR-11 | Điểm tổng là trung bình có trọng số của các chủ đề; kỹ năng bắt buộc trong JD có trọng số ×1.5 | 💡

BR-12 | Câu bỏ qua, không trả lời hoặc hết giờ được 0 điểm và tính là điểm yếu | 💡

BR-13 | Xếp loại tổng: từ 80 là Rất tốt; 65 đến 79 là Khá; 50 đến 64 là Trung bình; dưới 50 là Cần cải thiện | 💡


5.4 Kiến thức yếu

ID | Quy tắc | Nguồn

BR-14 | Chủ đề là yếu nếu điểm chủ đề dưới 50%, hoặc có từ 2 câu bị xếp Yếu. Mỗi chủ đề yếu gắn với các câu sai, ý chính còn thiếu và nội dung học liên quan | ✅


5.5 Dữ liệu và hệ thống

ID | Quy tắc | Nguồn

BR-15 | Chỉ ứng viên sở hữu mới xem được JD, phiên và kết quả của mình | 💡

BR-16 | Trạng thái phiên: CREATED, IN_PROGRESS, rồi COMPLETED, TIMED_OUT hoặc CANCELLED; chỉ phiên IN_PROGRESS nhận câu trả lời | 💡

BR-17 | Câu trả lời đã gửi không sửa được | 💡

BR-18 | Khi AI không chấm được sau số lần thử lại cho phép, lượt đó là UNGRADED, không tính vào điểm và hệ thống chuyển câu tiếp theo | 💡

BR-19 | Admin chỉ xuất bản câu hỏi khi đủ trường bắt buộc: chủ đề, level, nội dung, đáp án chuẩn, ý chính | 💡

BR-20 | Trong lúc phiên diễn ra, không API nào trả điểm hay nhận xét cho ứng viên; chỉ báo cáo sau khi phiên kết thúc mới hiển thị | ✅


## 6. Use case

ID | Use case | Actor | Ưu tiên | FR chính

UC-01 | Đăng ký, đăng nhập | Candidate, Admin | Must | FR-01

UC-02 | Nộp CV |  | Đã loại | FR-03

UC-03 | Nộp JD | Candidate | Must | FR-04, FR-42

UC-04 | Phân tích JD và xác nhận chủ đề | Candidate, AI | Must | FR-06, FR-07, FR-38

UC-05 | Cấu hình và bắt đầu phiên | Candidate | Must | FR-09, FR-10

UC-06 | Thực hiện phỏng vấn (chat có tính giờ) | Candidate, AI Interviewer | Must | FR-11, FR-15 đến FR-24

UC-07 | Đánh giá câu trả lời | AI Interviewer | Must | FR-17

UC-08 | Sinh follow-up | AI Interviewer | Must | FR-18, FR-20, FR-21

UC-09 | Chuyển câu hoặc chủ đề | AI Interviewer | Must | FR-19, FR-22

UC-10 | Kết thúc phiên và kích hoạt báo cáo | Candidate, Hệ thống | Must | FR-14, FR-43

UC-11 | Xem báo cáo và kiến thức yếu | Candidate | Must | FR-25, FR-26, FR-27

UC-12 | Xem nội dung ôn tập | Candidate | Must | FR-28, FR-31

UC-13 | Xem lịch sử và tiến bộ | Candidate | Should | FR-29, FR-30

UC-14 | Quản lý ngân hàng câu hỏi và danh mục chủ đề | Admin | Must | FR-32, FR-37, FR-39

UC-15 | Quản lý tài liệu và lập chỉ mục | Admin | Must | FR-33

UC-16 | Quản lý người dùng, xem thống kê | Admin | Must (người dùng), Could (thống kê) | FR-34, FR-35

UC-17 | Phỏng vấn bằng giọng nói | Candidate | Won't | FR-36

UC-18 | Khởi động hệ thống (warm-up) | Candidate, Hệ thống | Must | NFR-31


6.1 UC-06: Thực hiện phỏng vấn

Tiền điều kiện: phiên ở trạng thái CREATED, đã có JD với danh sách chủ đề được xác nhận và pool câu hỏi.

Luồng chính:

Hệ thống chọn câu hỏi đầu tiên từ pool theo BR-04, BR-08 và FR-22 (không lặp câu) và chuyển phiên sang IN_PROGRESS.

Hiển thị câu hỏi và bắt đầu đếm giờ.

Ứng viên gửi câu trả lời; đồng hồ dừng.

AI chấm điểm theo rubric (UC-07); điểm và lý do được lưu, không hiển thị.

Nếu thỏa đủ 4 điều kiện của BR-05, hệ thống dùng follow-up (UC-08) và quay lại bước 2.

Nếu không, hệ thống chọn câu hoặc chủ đề kế tiếp (UC-09) và quay lại bước 2.

Khi thỏa BR-09, hệ thống kết thúc phiên (UC-10).

Luồng thay thế: ứng viên bỏ qua câu (0 điểm theo BR-12); hết giờ; mất kết nối (khôi phục theo NFR-16); AI lỗi (retry, provider dự phòng, rồi UNGRADED theo BR-18).

Hậu điều kiện: toàn bộ hội thoại và điểm được lưu; phiên ở trạng thái kết thúc và báo cáo được kích hoạt.

6.2 UC-18: Khởi động hệ thống

Khi người dùng mở web, giao diện gọi endpoint warm-up; backend đánh thức AI service song song.

Giao diện hiển thị trạng thái đang khởi động và tự thử lại cho đến khi các dịch vụ sẵn sàng; không báo lỗi trong thời gian này (AC-23).

## 7. User story

Các user story của luồng Mock Interview và quản trị; US-01 (nộp CV) đã bị loại cùng FR-03.

ID | User story | Liên quan

US-02 | Là ứng viên, tôi muốn nộp JD để buổi phỏng vấn bám sát vị trí tôi ứng tuyển. | FR-04, FR-06

US-03 | Là ứng viên, tôi muốn xem và chỉnh các chủ đề và level hệ thống trích xuất từ JD để chắc chắn phạm vi phỏng vấn đúng ý. | FR-07

US-04 | Là ứng viên, tôi muốn chọn số câu và thời lượng để phù hợp với thời gian luyện tập của mình. | FR-10

US-05 | Là ứng viên, tôi muốn trả lời qua khung chat có đồng hồ đếm để mô phỏng áp lực phỏng vấn thật. | FR-11, FR-16

US-06 | Là ứng viên, tôi muốn mỗi câu trả lời được AI đánh giá để hệ thống biết nên hỏi tiếp thế nào, và kết quả đó chỉ hiện ở báo cáo cuối phiên. | FR-17, BR-20

US-07 | Là ứng viên, tôi muốn được hỏi sâu khi trả lời tốt để kiểm tra độ hiểu thật sự của mình. | FR-18, FR-20, FR-21

US-08 | Là ứng viên, tôi muốn được chuyển sang chủ đề khác khi trả lời chưa tốt để buổi phỏng vấn không bị mắc kẹt. | FR-19

US-09 | Là ứng viên, tôi muốn không bị hỏi lại câu đã hỏi để buổi luyện hiệu quả. | FR-22

US-10 | Là ứng viên, tôi muốn kết thúc phiên sớm khi cần dừng. | FR-14

US-11 | Là ứng viên, tôi muốn xem điểm tổng và điểm từng chủ đề sau khi phỏng vấn để biết mình đang ở đâu. | FR-25

US-12 | Là ứng viên, tôi muốn biết rõ các phần kiến thức yếu kèm nội dung cần ôn để tập trung học đúng chỗ. | FR-27, FR-28

US-13 | Là ứng viên, tôi muốn phiên không bị mất khi tải lại trang hoặc mất mạng ngắn để không phải làm lại. | NFR-16

US-14 | Là quản trị viên, tôi muốn thêm và sửa câu hỏi kèm đáp án chuẩn và ý chính để AI có dữ liệu truy xuất và chấm chính xác. | FR-32, FR-33

US-15 | Là quản trị viên, tôi muốn quản lý danh mục chủ đề và từ khóa đồng nghĩa để JD được ánh xạ đúng về câu hỏi trong hệ thống. | FR-37

US-16 | Là ứng viên, tôi muốn biết chủ đề nào trong JD hệ thống chưa hỗ trợ để hiểu phạm vi phỏng vấn. | FR-38

US-17 | Là ứng viên, tôi muốn vẫn nhận được điểm và kiến thức yếu dù AI lỗi lúc sinh nhận xét để không mất kết quả buổi luyện. | FR-43

US-18 | Là ứng viên, tôi muốn thấy hệ thống đang khởi động thay vì nhận thông báo lỗi khi mở web lần đầu. | NFR-31

US-19 | Là quản trị viên, tôi muốn khóa hoặc mở tài khoản người dùng để kiểm soát việc sử dụng quota AI. | FR-34


## 8. Tiêu chí nghiệm thu (AC)

Viết theo dạng Given, When, Then. AC-06 áp dụng cho hết giờ toàn phiên; hết giờ từng câu chỉ áp dụng khi FR-12 được triển khai.

ID | Tiêu chí | Liên quan

AC-01 | Given ứng viên đã đăng nhập, When dán JD hợp lệ, Then hệ thống hiển thị danh sách chủ đề và level trích xuất được trong tối đa 30 giây. | US-02, NFR-05

AC-02 | Given JD rỗng hoặc vượt giới hạn độ dài, When nộp, Then hệ thống báo lỗi rõ ràng và không tạo phiên. | US-02

AC-03 | Given JD đã phân tích, When ứng viên bật tắt chủ đề hoặc đổi level, Then phiên chỉ dùng danh sách sau khi chỉnh. | US-03

AC-04 | Given phiên đang diễn ra, When câu hỏi hiển thị, Then đồng hồ chạy; When AI đang xử lý, Then đồng hồ tạm dừng và giao diện hiển thị trạng thái đang xử lý. | US-05, NFR-04

AC-05 | Given ứng viên gửi câu trả lời, When AI chấm xong, Then hệ thống lưu điểm 4 tiêu chí, điểm câu và lý do theo định dạng hợp lệ, và không trả điểm hay nhận xét trong phản hồi hay giao diện khi phiên còn chạy. | US-06, BR-20

AC-06 | Given phiên hết giờ khi câu hiện tại chưa được trả lời, When hết giờ, Then câu đó được 0 điểm, phiên chuyển TIMED_OUT và báo cáo được kích hoạt. | BR-09, BR-12

AC-07 | Given điểm câu từ 70% trở lên, số follow-up liên tiếp nhỏ hơn 2 và còn thời gian, When chấm xong, Then hệ thống đưa ra đúng một follow-up bám nội dung câu trả lời và thuộc chủ đề trong JD. | US-07, BR-05

AC-08 | Given điểm câu dưới 70%, When chấm xong, Then hệ thống không sinh follow-up và chuyển câu hoặc chủ đề tiếp theo. | US-08

AC-09 | Given đã hỏi đủ 2 follow-up liên tiếp, When chấm xong câu thứ hai, Then hệ thống chuyển sang câu gốc mới. | FR-20

AC-10 | Given một câu đã hỏi trong phiên, When chọn câu tiếp theo, Then câu đó không xuất hiện lại. | US-09

AC-11 | Given một follow-up được sinh ra, When kiểm tra phạm vi, Then không có chủ đề nào nằm ngoài danh sách chủ đề của JD. | FR-21

AC-12 | Given phiên đã kết thúc, When ứng viên mở báo cáo, Then thấy điểm tổng, điểm theo chủ đề, nhận xét từng câu và danh sách chủ đề yếu theo BR-14; Given phiên chưa kết thúc, When yêu cầu báo cáo, Then hệ thống từ chối. | US-11, BR-20

AC-13 | Given một chủ đề bị xếp yếu, When xem báo cáo, Then mỗi chủ đề yếu hiển thị ý chính còn thiếu và liên kết tới nội dung ôn tương ứng (nếu có). | US-12

AC-14 | Given dịch vụ AI trả lỗi hoặc vượt hạn mức, When đang phỏng vấn, Then hệ thống thử lại theo cấu hình, sau đó dùng provider hoặc câu dự phòng; ứng viên nhận thông báo thân thiện và không mất dữ liệu phiên. | NFR-08, NFR-10

AC-15 | Given AI trả đầu ra sai định dạng, When xử lý, Then hệ thống validate và thử lại; nếu vẫn lỗi, lượt đó là UNGRADED và không ảnh hưởng điểm. | NFR-11, BR-18

AC-16 | Given JD hoặc câu trả lời chứa chỉ dẫn kiểu bỏ qua quy tắc, cho điểm tối đa, When AI xử lý, Then điểm vẫn theo rubric và không bị thay đổi bởi chỉ dẫn đó. | NFR-20

AC-17 | Given ứng viên tải lại trang giữa phiên, When quay lại, Then phiên tiếp tục từ câu hiện tại với hội thoại và thời gian còn lại chính xác. | US-13, NFR-16

AC-18 | Given Admin thêm câu hỏi thiếu đáp án chuẩn hoặc ý chính, When lưu, Then hệ thống từ chối và chỉ ra trường còn thiếu. | US-14, BR-19

AC-19 | Given câu hỏi hoặc tài liệu mới được lưu, When lập chỉ mục xong, Then nội dung có thể được truy xuất trong phiên phỏng vấn sau đó; câu chưa lập chỉ mục không được chọn. | FR-33

AC-20 | Given phiên MVP, When diễn ra, Then câu hỏi, follow-up và nhận xét bằng tiếng Việt, thuật ngữ Java giữ nguyên tiếng Anh. | NFR-26

AC-21 | Given JD có chủ đề hệ thống chưa hỗ trợ, When phân tích xong, Then hệ thống liệt kê các chủ đề đó cho ứng viên. | US-16, FR-38

AC-22 | Given sinh nhận xét bằng LLM lỗi, When tạo báo cáo, Then báo cáo ở trạng thái PARTIAL vẫn hiển thị điểm, ý chính còn thiếu và tài liệu ôn. | US-17, FR-43

AC-23 | Given hệ thống đang khởi động nguội, When người dùng mở web, Then giao diện hiển thị trạng thái chờ và tự thử lại, không báo lỗi. | US-18, NFR-31


## 9. Phạm vi MVP và phân loại MoSCoW

9.1 Tuyên bố MVP

Admin nạp ngân hàng câu hỏi, tài liệu và danh mục chủ đề. Người dùng dán JD, xác nhận chủ đề, rồi phỏng vấn bằng chat có tính giờ, trong đó AI chấm từng câu, hỏi sâu khi trả lời tốt và chuyển chủ đề khi chưa đạt. Sau đó người dùng nhận báo cáo điểm, danh sách kiến thức yếu và nội dung ôn tập tương ứng. Toàn bộ bằng tiếng Việt, chỉ dùng mô hình và API miễn phí hoặc mã nguồn mở.

9.2 Trong phạm vi và ngoài phạm vi

Trong MVP | Ngoài MVP

Đăng nhập, phân quyền Candidate và Admin | Voice (STT, TTS)

Dán JD, phân tích, chỉnh chủ đề | CV, tải file JD

Phỏng vấn chat có đồng hồ, hỏi sâu và chuyển chủ đề | Biểu đồ tiến bộ, xuất PDF

Chấm điểm theo rubric, báo cáo, kiến thức yếu | Tiếng Anh

Admin: câu hỏi, tài liệu, chủ đề, người dùng | Luyện tập không cần JD

Chịu được lỗi và giới hạn của API miễn phí | Thống kê nâng cao cho Admin


9.3 Phân loại MoSCoW

Mức | FR

Must (28) | FR-01, 04, 06, 07, 09, 10, 11, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 32, 33, 34, 37, 38, 43

Should (6) | FR-02, 12, 29, 31, 39, 42

Could (5) | FR-13, 30, 35, 40, 41

Won't (1) | FR-36


NFR Must gồm NFR-01, 02, 04 đến 11, 12 đến 17, 19, 20, 23 đến 33. NFR Should gồm NFR-18, 21, 22. NFR Could gồm NFR-03.

9.4 Dữ liệu tối thiểu để MVP chạy được

Khoảng 8 chủ đề (ví dụ OOP, Core Java, Collections, Java 8+ Lambda và Stream, Concurrency, JVM và Memory, Spring Boot, Database và JPA).

Khoảng 15 câu hỏi mỗi chủ đề (khoảng 120 câu), mỗi câu có đáp án chuẩn và ý chính bằng tiếng Việt.

Ít nhất một tài liệu ôn tập cho mỗi chủ đề.

9.5 Tiêu chí hoàn thành MVP

Chạy trọn kịch bản từ đầu đến cuối không lỗi: dán JD, phiên chat, báo cáo, mở nội dung ôn tập.

Trong một phiên có đủ cả nhánh hỏi sâu và nhánh chuyển chủ đề.

Đạt các AC cốt lõi: AC-01 đến AC-23 (trừ AC phụ thuộc FR Should chưa làm).

Thời gian phản hồi AI nằm trong NFR-02 với mô hình đã chọn.

Admin tự nạp được dữ liệu qua giao diện, không cần chạy script.

9.6 Ba tầng ưu tiên và thứ tự xây dựng

Tầng | Gồm | Ý nghĩa

Tầng 1: bắt buộc cho đồ án | 28 Must FR và NFR Must | Thiếu một mục là chưa đạt MVP

Tầng 2: nâng cao nếu còn thời gian | 6 Should và 5 Could | Làm Should trước, Could sau

Tầng 3: phát triển sau đồ án | Voice, tiếng Anh, CV và các mục Won't khác | Thiết kế để mở rộng được nhưng chưa làm


Thứ tự xây dựng theo lát cắt chức năng:

Nền tảng nội dung: đăng nhập và Admin quản lý chủ đề, câu hỏi, tài liệu (FR-01, 32, 33, 37).

Từ JD đến phiên: nộp, phân tích, chỉnh chủ đề, tạo phiên (FR-04, 06, 07, 09, 10, 38).

Lõi phỏng vấn: chat, đồng hồ, RAG, chấm điểm, hỏi sâu và chuyển câu (FR-11, 15 đến 24).

Kết quả và học: báo cáo, kiến thức yếu, nội dung ôn, kết thúc sớm, quản lý người dùng (FR-14, 25 đến 28, 34, 43).

9.7 Rủi ro về phạm vi

Chất lượng LLM miễn phí cho tiếng Việt là rủi ro lớn nhất của FR-17 và FR-18; cần thử nghiệm mô hình sớm (NFR-28).

Khối lượng nội dung (khoảng 120 câu kèm đáp án chuẩn và ý chính) ngang với phần lập trình.

28 Must cho một người làm là khá nặng. Nếu trễ, rút gọn trước FR-07 (chỉ bật tắt chủ đề), FR-28 (chỉ danh sách tài liệu), FR-26 (chỉ lý do chấm), không bỏ hẳn.

## 10. Thiết kế hệ thống

10.1 Kiến trúc tổng thể

Trình duyệt tải frontend tĩnh từ Vercel và gọi REST tới Java Backend trên Render. Java Backend gọi Python AI Service trên Koyeb và đọc ghi Neon Postgres (schema app). Python AI Service đọc ghi cùng cơ sở dữ liệu ở schema rag (vector và chunk) và gọi API Gemini (chính) hoặc Groq (dự phòng). Nguyên tắc: mọi quyết định mang tính luật nằm ở Java, Python chỉ trả dữ liệu có cấu trúc và không giữ trạng thái phiên.

Thành phần | Vai trò | Không làm

Frontend (React + Vite, bản build tĩnh) | Đăng nhập; nhập JD và xác nhận chủ đề; cấu hình phiên; giao diện chat; hiển thị đồng hồ (chỉ để xem); polling trạng thái phiên mỗi khoảng 2 giây; trang báo cáo và nội dung ôn tập; màn hình Admin | Không gọi trực tiếp AI service hay LLM; không giữ API key

Java Backend (Spring Boot) | Xác thực JWT, phân quyền; quản lý nội dung; điều phối phiên; đồng hồ chuẩn; toàn bộ luật BR; quét phiên hết giờ; gọi AI service có timeout, retry, circuit breaker; sở hữu schema app | Không gọi LLM; không xử lý vector

Python AI Service (FastAPI, không trạng thái) | Chunking, embedding, lập chỉ mục, truy xuất RAG; phân tích JD; chấm câu trả lời; sinh follow-up; sinh nhận xét báo cáo; cổng LLM với provider dự phòng; validate JSON; bảo vệ quota; sở hữu schema rag | Không quyết định luật hỏi sâu hay chuyển câu; không tính điểm tổng; không giữ trạng thái phiên


Frontend chốt React (⚠️ chưa xác nhận cuối cùng, chủ dự án đã nêu React hoặc Vue).

10.2 Chia module

Module | Chạy ở | Trách nhiệm | FR và NFR chính

M1 Identity & Access | Java | Đăng ký, đăng nhập, JWT, phân quyền, khóa mở tài khoản | FR-01, 02, 34; NFR-17, 19

M2 Content Management | Java | Admin quản lý chủ đề, câu hỏi (kèm đáp án chuẩn, ý chính), tài liệu | FR-32, 33, 37, 39

M3 Indexing & Retrieval (RAG) | Python | Cắt chunk, tạo embedding, truy xuất câu hỏi và đoạn tài liệu | FR-15, 33; NFR-15, 28

M4 JD Analysis | Python và Java | Trích xuất chủ đề và level, ánh xạ về danh mục chủ đề, xác nhận | FR-04, 06, 07, 38

M5 Interview Orchestrator | Java | Máy trạng thái phiên, đồng hồ, chọn câu, luật hỏi sâu hay chuyển câu, transcript | FR-09, 10, 11, 14, 16, 19 đến 24; BR-04 đến 09, 16, 17

M6 AI Evaluation & Follow-up | Python | Chấm điểm theo rubric, sinh follow-up, validate JSON | FR-17, 18, 21, 24; NFR-11, 13, 14, 20

M7 Reporting & Weak Knowledge | Java | Tính điểm chủ đề và tổng, xác định kiến thức yếu, gắn nội dung ôn, báo cáo bất đồng bộ | FR-25 đến 28, 43; BR-10 đến 14

M8 AI Gateway | Python | Trừu tượng hóa nhà cung cấp LLM, rate limit, retry, fallback, log mỗi lần gọi | NFR-07 đến 10, 24


10.3 Workflow Mock Interview

Bước | Thành phần | Mô tả

A1 | FE, Java, Python | Khi mở web, FE gọi GET /system/warmup; Java đánh thức AI service bằng /health song song

A2 | FE, Java | Người dùng đăng nhập và dán JD (POST /job-descriptions, trả 202)

A3 | Java, Python | Java gửi JD kèm danh mục chủ đề và alias; Python dùng LLM trích xuất và ánh xạ, trả chủ đề hỗ trợ, chưa hỗ trợ và level

A4 | FE, Java | FE polling đến khi JD READY; người dùng bật tắt chủ đề, chỉnh level, xác nhận

A5 | FE, Java, Python | Người dùng cấu hình số câu và thời lượng; Java gọi /retrieve/question-pool một lần và lưu pool; phiên CREATED

B1 | Java | POST /sessions/{id}/start: chọn câu đầu từ pool, phiên IN_PROGRESS, đồng hồ bắt đầu

B2 | FE | Polling GET /sessions/{id}/state để hiện câu hỏi và thời gian còn lại

B3 | FE, Java | Gửi câu trả lời (POST /answers, kèm requestId); Java dừng đồng hồ lượt, lưu, trả 202; trạng thái WAITING_AI

B4 | Java, Python | Java gọi /evaluate/turn; Python lấy ngữ cảnh, gọi LLM, trả JSON; Java validate, tính điểm theo BR-01 và lưu, không công bố

B5 | Java | Áp BR-05: dùng follow-up hoặc lấy câu kế từ pool; kiểm tra BR-09; trạng thái AWAITING_ANSWER hoặc ENDED

B6 | Java, Python | Lỗi 429 hoặc 503: retry có backoff, thử Groq, nếu vẫn lỗi thì UNGRADED và chuyển câu từ pool (BR-18); phiên quá hạn do lịch quét xử lý (BR-12)

C1 | Java | Khi kết thúc, tính ngay điểm chủ đề, điểm tổng và chủ đề yếu (BR-10 đến 14); báo cáo GENERATING

C2 | Java, Python | Một lệnh gọi /report/generate cho cả phiên để sinh nhận xét và tổng kết; thành công thì READY, lỗi thì PARTIAL

C3 | FE | Hiển thị báo cáo; chủ đề yếu mở tài liệu qua /topics/{id}/study-materials


Ba tối ưu rút ra từ ràng buộc nguồn lực: pool câu hỏi tính một lần nên trong lúc chat không cần gọi embedding; chấm trong phiên chỉ trả dữ liệu gọn còn nhận xét dài dồn vào một lệnh gọi lúc kết thúc; báo cáo có trạng thái PARTIAL nên LLM lỗi lúc cuối không làm mất kết quả.

10.4 Input, Output và API

Ranh giới | Vào | Ra

FE sang Java (REST, JSON, Bearer JWT) | Thông tin đăng nhập, văn bản JD, cấu hình phiên, câu trả lời, thao tác Admin | Trạng thái phiên, kết quả phân tích JD, báo cáo, nội dung ôn; không bao giờ có điểm khi phiên chưa kết thúc

Java sang Python (REST nội bộ) | JD và danh mục chủ đề; nội dung để lập chỉ mục; dữ liệu một lượt; dữ liệu cả phiên | JSON có cấu trúc: chủ đề, pool câu hỏi, điểm 4 tiêu chí, ý chính đạt hoặc thiếu, follow-up, nhận xét

Python sang LLM và Embedding API | Prompt theo mẫu ngôn ngữ, yêu cầu JSON | Văn bản hoặc JSON thô, được validate

Python sang Neon | Vector và chunk (schema rag) | Kết quả truy xuất theo độ tương đồng

Java sang Neon | Dữ liệu nghiệp vụ (schema app) | Bản ghi nghiệp vụ


API công khai (Frontend và Java), tiền tố /api/v1

Nhóm | Endpoint | Ghi chú

Hệ thống | GET /system/warmup | Không cần đăng nhập; trả trạng thái backend và AI service

Auth | POST /auth/register, /auth/login, /auth/refresh | 

JD | POST /job-descriptions; GET /job-descriptions/{id}; PUT /job-descriptions/{id}/topics | Nộp JD (202), xem trạng thái và kết quả, xác nhận chủ đề và level

Phiên | POST /sessions; POST /sessions/{id}/start; POST /sessions/{id}/answers; POST /sessions/{id}/skip; POST /sessions/{id}/finish | Tạo, bắt đầu, gửi câu trả lời (202, kèm requestId), bỏ qua, kết thúc sớm

Phiên | GET /sessions/{id}/state | Polling: AWAITING_ANSWER, WAITING_AI hoặc ENDED, câu hiện tại, thời gian còn lại; không chứa điểm (BR-20)

Báo cáo | GET /sessions/{id}/report; GET /topics/{id}/study-materials | Báo cáo chỉ truy cập được khi phiên đã kết thúc; nội dung ôn tập

Admin | /admin/topics, /admin/questions, /admin/documents (CRUD); POST /admin/questions/{id}/publish; POST /admin/documents/{id}/reindex | Quản lý nội dung, xuất bản, lập chỉ mục lại

Admin | GET /admin/users; PATCH /admin/users/{id}/status; GET và PUT /admin/settings/scoring | Quản lý người dùng, ngưỡng và trọng số

Should | GET /sessions, PATCH /me, POST /admin/questions/import, POST /job-descriptions/upload | Lịch sử, hồ sơ, nhập hàng loạt, tải file JD


API nội bộ (Java và Python AI Service), tiền tố /ai/v1

Endpoint | Vào | Ra

GET /health | (không có) | Trạng thái, tình trạng từng provider và DB; dùng cho warm-up

POST /jd/analyze | jdText, language, danh mục topics (code, name, aliases) | level, topics (code, importance, evidence), unsupportedTerms

POST /index/questions | questionId, topicCode, language, level, content, referenceAnswer, keyPoints | indexed, contentHash (kèm DELETE /index/questions/{id})

POST /index/documents | documentId, topicCode, language, title, contentMd | chunkCount (kèm DELETE /index/documents/{id})

POST /retrieve/question-pool | jdId, jdText, topics (code, weight), level, perTopic | pool (questionId, topicCode, rank, score)

POST /evaluate/turn | turnId, questionText, referenceAnswer, keyPoints, topicCode, level, answerText, groundingQuestionId, wantFollowUp, allowedTopicCodes, language | scores (accuracy, completeness, clarity, example), keyPoints (covered, missed), followUp (text, topicCode) hoặc null, meta

POST /report/generate | language, topics, turns (seq, questionText, answerText, scores, missedKeyPoints) | turnFeedback, topicSummaries, overallSummary

POST /retrieve/documents (Should) | topicCode, missedKeyPoints, limit | items (documentId, chunkIndex, score)


Quy ước cho API nội bộ: xác thực bằng header X-Internal-Key kèm X-Request-Id; Java chờ /evaluate/turn tối đa khoảng 35 giây và /health khi đánh thức cold start tối đa khoảng 90 giây; mã lỗi 422 (LLM sai định dạng sau khi thử lại), 429 kèm Retry-After (hết quota), 503 (provider không khả dụng); Python không lưu trạng thái, chống gửi trùng theo turnId do Java đảm nhiệm.

10.5 Cơ sở dữ liệu

PostgreSQL (Neon) với pgvector. Java sở hữu schema app, Python sở hữu schema rag; liên kết giữa questions, documents và các bảng rag_* là liên kết logic, không có khóa ngoại xuyên schema.

Bảng | Schema | Thuộc tính chính

users | app | id, email, role, status

topics | app | id, code, parent_id, is_active

topic_translations | app | topic_id, language, name, description

topic_aliases | app | id, topic_id, alias

questions | app | id, topic_id, language, level, content, reference_answer, status

question_key_points | app | id, question_id, seq, content

documents | app | id, topic_id, language, title, content_md

job_descriptions | app | id, user_id, raw_text, level, status

jd_topics | app | id, jd_id, topic_id (có thể rỗng), raw_term, importance, is_selected

interview_sessions | app | id, user_id, jd_id, status, time_limit_sec, elapsed_ms, config_snapshot, total_score

session_topics | app | session_id, topic_id, weight, topic_score, is_weak

session_question_pool | app | session_id, question_id, rank, used

session_turns | app | id, session_id, parent_turn_id, question_id, kind, question_text, answer_text, status

turn_evaluations | app | turn_id, accuracy, completeness, clarity, example, total_score, missed_key_points, feedback

session_reports | app | session_id, status, summary, generated_at

ai_call_logs | app | session_id, turn_id, purpose, provider, model, status, latency_ms, error

scoring_settings | app | key, value (JSON), updated_by, updated_at

rag_question_vectors | rag | question_id, embedding, content_hash

rag_document_chunks | rag | id, document_id, chunk_index, chunk_text, embedding


Quan hệ chính:

users 1-n job_descriptions và users 1-n interview_sessions.

topics 1-n topic_aliases, questions, documents; questions 1-n question_key_points.

job_descriptions 1-n jd_topics và 1-n interview_sessions; topics 0..1-n jd_topics.

interview_sessions 1-n session_topics, session_question_pool, session_turns; 1-0..1 session_reports.

session_turns 1-0..1 turn_evaluations; session_turns tham chiếu chính nó qua parent_turn_id (follow-up trỏ về lượt gốc).

questions 1-0..1 rag_question_vectors; documents 1-n rag_document_chunks.

Ghi chú thiết kế: session_question_pool là kết quả RAG tính một lần lúc tạo phiên; session_turns.question_text lưu nguyên văn câu đã hỏi vì follow-up không có bản gốc trong questions; turn_evaluations.feedback để trống đến khi báo cáo sinh xong; interview_sessions.config_snapshot giữ ngưỡng và trọng số tại thời điểm tạo phiên; dung lượng ước tính khoảng 2.000 vector 768 chiều chiếm cỡ 6 MB (⚠️), ở quy mô này chưa cần chỉ mục vector; kết nối dùng pool nhỏ qua pooler của Neon, có retry khi compute vừa thức dậy.

10.6 Triển khai

Phương án: Vercel cho frontend, Render cho Java, Koyeb cho Python, Neon cho database, Gemini là provider chính và Groq dự phòng.

Thành phần | Nền tảng | Lý do | Giới hạn cần biết

Frontend | Vercel | Bản build tĩnh, ít tốn tài nguyên | ⚠️ Chưa kiểm tra lại điều khoản gói Hobby; thay thế: Cloudflare Pages hoặc Netlify

Java Backend | Render, web service miễn phí (Docker) | Deploy thẳng từ GitHub | 512 MB RAM, ngủ sau 15 phút, mất khoảng một phút để thức

Python AI Service | Koyeb, web service miễn phí | Chỉ một service miễn phí, vừa đủ cho service Python nhẹ | Có thể bị yêu cầu thẻ, tự về 0 sau một giờ không có truy cập

Database | Neon (Postgres + pgvector) | Không hết hạn, có pgvector, có nhánh dev | Compute ngủ sau 5 phút

LLM và Embedding | Gemini Flash (chính), Groq (dự phòng) | Đổi qua lại nhờ lớp adapter (NFR-07) | Hạn mức khác nhau giữa các nguồn, phải đo thực tế


Phương án dự phòng nếu Koyeb không phù hợp: chạy Python như dịch vụ miễn phí thứ hai trên Render. Nếu hai service vẫn không chạy nổi trong 512MB, đường lui cuối là gộp Java và Python vào một container (không đề xuất làm ngay).

Giới hạn hosting và API miễn phí hiện hành (tra cứu tháng 10/2026)

Render: dịch vụ web miễn phí có 512 MB RAM và 0,1 CPU, ngủ sau 15 phút, mất khoảng một phút để chạy lại; các dịch vụ miễn phí dùng chung 750 giờ mỗi tháng; Postgres miễn phí hết hạn sau 30 ngày kèm 14 ngày ân hạn.

Koyeb: một web service miễn phí mỗi tổ chức (512 MB RAM, 0,1 vCPU), tự về 0 sau một giờ, có thể yêu cầu thẻ; Koyeb được Mistral AI mua lại từ tháng 2/2026 nhưng vẫn hoạt động; Postgres miễn phí chỉ có 5 giờ hoạt động mỗi tháng.

Railway: chỉ có 5 USD dùng thử trong 30 ngày rồi còn 1 USD credit mỗi tháng, không đủ cho JVM chạy thường trực nên không khuyến nghị.

Neon: gói miễn phí có pgvector, 100 CU-giờ mỗi tháng, compute ngủ sau 5 phút, không cần thẻ, không hết hạn, dung lượng 1 GB mỗi project.

Gemini API: gói miễn phí chỉ còn Flash và Flash-Lite từ tháng 4/2026; hạn mức các nguồn không thống nhất (khoảng 100 đến 1.500 yêu cầu mỗi ngày cho Flash); prompt ở gói miễn phí có thể được dùng để huấn luyện mô hình; không dành cho mục đích thương mại.

Groq: khoảng 30 yêu cầu mỗi phút và 14.400 yêu cầu mỗi ngày tùy mô hình; từ giữa năm 2026 chuyển sang giới hạn ngày theo từng mô hình.

Ngân sách tài nguyên ⚠️

Java (512 MB): heap khoảng 256 đến 320 MB, SerialGC, khởi tạo bean lười; khởi động trên 0,1 CPU có thể mất 1 đến 2 phút (ước tính, cần đo sớm).

Python (khoảng 150 đến 250 MB): FastAPI, httpx, driver Postgres; không dùng torch hay sentence-transformers.

Neon: 100 CU-giờ tương đương khoảng 400 giờ hoạt động ở cỡ nhỏ nhất 0,25 CU.

LLM: khoảng 15 lệnh gọi mỗi phiên (1 phân tích JD, khoảng 10 đến 12 lượt chấm, 1 báo cáo, cộng chút dự phòng); embedding chỉ phát sinh khi Admin lập chỉ mục và một lần cho mỗi JD.

Khởi động trước khi demo và môi trường phát triển ⚠️

Warm-up gọi các tầng song song (Render, Koyeb, Neon) để tổng thời gian chờ chỉ bằng tầng chậm nhất; nên mở web trước vài phút khi demo; không dùng công cụ ping giữ thức 24/7 vì một dịch vụ chạy liên tục dùng gần hết 750 giờ mỗi tháng của Render.

Phát triển trên máy 4GB: không dùng Docker Desktop và không chạy Postgres cục bộ, dùng nhánh Neon cho dev; chạy Java và Python trực tiếp; thêm chế độ provider giả lập trong AI Gateway để kiểm thử không tốn quota; GitHub làm nguồn, mỗi nền tảng tự deploy khi có commit mới; khóa API đặt trong biến môi trường.

## 11. Ma trận truy vết

Nhóm | FR | UC | US | AC | NFR và BR liên quan

Tài khoản | FR-01, 02 | UC-01 | (chức năng nền) | (không có) | NFR-17, 19

Đầu vào JD | FR-04, 06, 07, 38, 42 | UC-03, 04 | US-02, 03, 16 | AC-01, 02, 03, 21 | NFR-05; BR-07

Cấu hình và điều phối phiên | FR-09 đến 14 | UC-05, 10 | US-04, 05, 10 | AC-04, 06 | NFR-04; BR-09, 16

Phỏng vấn lõi | FR-15 đến 24 | UC-06 đến 09 | US-05 đến 09 | AC-05, 07 đến 11, 14 đến 16 | NFR-02, 10, 11, 13 đến 15, 20; BR-01 đến 08, 18

Báo cáo và kiến thức yếu | FR-25 đến 28, 43 | UC-10, 11, 12 | US-11, 12, 17 | AC-12, 13, 22 | BR-10 đến 14, 20

Khôi phục và khởi động | (không có) | UC-18 | US-13, 18 | AC-17, 23 | NFR-16, 31

Admin | FR-32 đến 35, 37, 39 | UC-14, 15, 16 | US-14, 15, 19 | AC-18, 19 | BR-19

Ngôn ngữ | (không có) | (không có) | (không có) | AC-20 | NFR-26 đến 28

Nâng cao, ngoài MVP | FR-29 đến 31, 36, 40, 41 | UC-13, 17 | (không có) | (không có) | NFR-03


## 12. Rủi ro chính

Rủi ro | Cách giảm thiểu

Java khởi động chậm trên 0,1 CPU | Tinh chỉnh JVM, khởi tạo lười, đo thời gian khởi động ngay từ bản đầu

Cold start dây chuyền (Render, Koyeb, Neon) | Warm-up song song, giao diện báo trạng thái, tự thử lại (NFR-31, AC-23)

Hạn mức LLM thay đổi hoặc thấp hơn kỳ vọng | Chuỗi provider Gemini rồi Groq, bộ đếm quota, giới hạn phiên mỗi ngày (NFR-32)

Chất lượng tiếng Việt của mô hình miễn phí | Chạy bộ kiểm thử nhỏ ở giai đoạn đầu để chọn mô hình (NFR-28)

Nền tảng đổi chính sách (Koyeb vừa đổi chủ, Render đổi gói trong 2026) | Đóng gói bằng Docker để chuyển nền tảng dễ

Dữ liệu gửi tới gói miễn phí có thể được dùng để huấn luyện | Thông báo cho người dùng, khuyên không nhập thông tin cá nhân vào JD (NFR-21)

Khối lượng nội dung câu hỏi lớn so với một người làm | Làm tuần tự theo lát cắt chức năng; rút gọn FR-07, FR-26, FR-28 nếu trễ


## 13. Giả định và vấn đề còn mở

# | Nội dung | Cách chốt

1 | Đối tượng người dùng chính (sinh viên năm cuối, fresher, junior Java) chưa được xác nhận | Chủ dự án xác nhận hoặc điều chỉnh

2 | JD đầu vào có thể là tiếng Việt hoặc tiếng Anh, phỏng vấn xuất ra tiếng Việt | Xác nhận khi thử nghiệm với JD thực tế

3 | Frontend chốt React hay Vue | Chủ dự án chọn trước khi bắt đầu lập trình

4 | Hạn mức Gemini và Groq thực tế, mô hình được chọn | Đọc trang giới hạn của nhà cung cấp, chạy bộ kiểm thử NFR-28

5 | Điều khoản gói Hobby của Vercel chưa kiểm tra | Kiểm tra khi deploy

6 | Koyeb có yêu cầu thẻ tín dụng hay không | Nếu có thì chuyển Python sang Render

7 | Thời gian khởi động JVM trên 0,1 CPU chưa đo | Đo ở bản deploy đầu tiên

8 | Các ngưỡng và mục tiêu (70%, tối đa 2 follow-up, ±10%, 15 và 30 giây, tối đa 5 phiên đồng thời) | Tinh chỉnh khi test thực tế

9 | Chính sách lưu trữ và xóa dữ liệu người dùng (thời gian giữ JD và transcript) chưa quy định | Quyết định khi triển khai NFR-18

10 | Nguồn biên soạn khoảng 120 câu hỏi và tài liệu ôn tập | Chủ dự án lập kế hoạch nội dung

11 | Thiết kế Giai đoạn 4 được xem là chấp thuận khi chủ dự án yêu cầu chuyển sang Giai đoạn 5 | Điều chỉnh nếu có phản hồi


## 14. Nhật ký quyết định

Giai đoạn | Quyết định | Nguồn

1 | Dự án cá nhân, quy mô tương đương đồ án tốt nghiệp | Chủ dự án

1 | Luồng cốt lõi: JD, câu hỏi RAG, chấm từng câu, hỏi sâu hoặc chuyển chủ đề | Chủ dự án

1 | Chỉ dùng API, LLM mã nguồn mở hoặc dịch vụ miễn phí; MVP chỉ có chat, Voice làm sau | Chủ dự án

2 | Admin bắt buộc và nằm trong MVP; chỉ cần JD, không cần CV | Chủ dự án

2 | Chốt tạm ngưỡng 70%, tối đa 2 follow-up, rubric 50/30/10/10, ngưỡng yếu 50% | Chủ dự án

2 | Tiếng Việt là chính, mở rộng tiếng Anh sau; kỹ năng khác nghĩa là chủ đề kiến thức | Chủ dự án

3 | Đồng ý phạm vi MVP (28 Must, 6 Should, 5 Could, Voice là Won't), BR-10 đến BR-13 và mức dữ liệu tối thiểu | Chủ dự án

4 | Máy 4GB không GPU; dùng API LLM miễn phí; deploy hosting miễn phí; tách Java Backend và Python AI Service; React hoặc Vue | Chủ dự án

4 | Điểm và nhận xét từng câu chỉ hiển thị ở báo cáo sau khi phiên kết thúc | Chủ dự án

5 | Chuyển sang tổng hợp SRS; thiết kế Giai đoạn 4 được chấp thuận ngầm | Chủ dự án


## Phụ lục A. Tài liệu liên quan

Thiết kế hệ thống: Ứng dụng ôn Java và phỏng vấn mô phỏng theo JD: bản thiết kế Giai đoạn 4 đã trình bày trước khi tổng hợp SRS.
