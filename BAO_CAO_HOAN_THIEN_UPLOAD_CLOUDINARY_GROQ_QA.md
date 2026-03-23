# Báo Cáo Hoàn Thiện Chức Năng Upload Tài Liệu Lên Cloudinary Và Hỏi Đáp Nhanh Bằng Groq AI

## 1) Mục tiêu và phạm vi thực hiện

Tài liệu này tổng hợp chi tiết toàn bộ các hạng mục đã hoàn thiện liên quan đến 2 năng lực trọng tâm:

1. Upload file hướng dẫn sinh hoạt lớp lên Cloudinary.
2. Tích hợp Groq AI API để hỏi đáp nhanh dựa trên nội dung tài liệu PDF.

Mục tiêu nghiệp vụ cần đạt:

- Người dùng upload tài liệu PDF thành công từ giao diện quản trị.
- Tài liệu sau upload có thể mở trực tiếp qua link lưu trong hệ thống.
- Người dùng nhập câu hỏi ngắn, hệ thống đọc tài liệu và trả lời bằng AI.
- Trải nghiệm người dùng mượt: bố cục hợp lý, đa ngôn ngữ, dark theme dễ đọc.
- Quy trình cấu hình an toàn hơn thông qua biến môi trường (env vars), hạn chế hardcode secret.

Phạm vi đã làm:

- Backend Spring Boot: service upload, service QA, endpoint API, xử lý lỗi, cấu hình.
- Frontend Angular: tích hợp panel hỏi đáp, gọi API, hiển thị kết quả, UX/UI.
- Cấu hình vận hành: Cloudinary, Groq, biến môi trường local và production.
- Xử lý lỗi thực tế phát sinh trong quá trình chạy thật với dữ liệu thật.

---

## 2) Kiến trúc chức năng sau khi hoàn thiện

### 2.1 Luồng upload tài liệu

1. Người dùng chọn PDF tại màn hình thêm hướng dẫn sinh hoạt.
2. Frontend gửi multipart/form-data về backend.
3. Backend nhận file, gọi Cloudinary upload với `resource_type=raw`, lưu vào thư mục `pdf_files`.
4. Backend nhận URL trả về từ Cloudinary, lưu vào DB trong bản ghi guide.
5. Khi truy vấn danh sách guide, backend trả link tài liệu để frontend hiển thị nút mở tài liệu.

### 2.2 Luồng hỏi đáp nhanh bằng AI

1. Người dùng nhập câu hỏi ở panel "Hỏi đáp nhanh".
2. Frontend gửi request đến endpoint QA kèm `activityId` và `question`.
3. Backend lấy danh sách guide thuộc hoạt động tương ứng.
4. Backend tải nội dung PDF, trích xuất text bằng PDFBox.
5. Backend dựng prompt theo ngữ cảnh tài liệu + câu hỏi người dùng.
6. Backend gọi Groq Chat Completions API.
7. Backend trả câu trả lời + danh sách tham chiếu tài liệu đã dùng.
8. Frontend hiển thị câu trả lời ngay trong panel.

---

## 3) Các hạng mục backend đã hoàn thiện

## 3.1 Chuẩn hóa cấu hình qua biến môi trường

Đã chuyển dần về cơ chế đọc env vars cho thông tin nhạy cảm:

- Cloudinary:
  - `CLOUDINARY_CLOUD_NAME`
  - `CLOUDINARY_API_KEY`
  - `CLOUDINARY_API_SECRET`
- Groq:
  - `GROQ_BASE_URL`
  - `GROQ_API_KEY`
  - `GROQ_MODEL`

Ý nghĩa:

- Không phụ thuộc giá trị hardcode trong mã nguồn.
- Dễ cấu hình giữa local/dev/prod.
- Giảm rủi ro lộ secret trong repository.

## 3.2 Hoàn thiện dịch vụ upload Cloudinary

Đã triển khai và hoàn thiện luồng upload PDF qua Cloudinary service:

- Validate file đầu vào:
  - Không null.
  - Không rỗng.
  - Đúng định dạng `.pdf`.
- Chuyển `MultipartFile` sang file tạm.
- Gọi Cloudinary upload với tham số phù hợp file raw.
- Trả URL file về service nghiệp vụ để lưu DB.
- Dọn dẹp file tạm, log lỗi rõ ràng khi thất bại.

Điểm quan trọng:

- Đã đảm bảo file được lưu ở Cloudinary thay vì chỉ lưu local path như trước.
- Link guide trả ra cho frontend được chuẩn hóa để tối ưu khả năng truy cập.

## 3.3 Tích hợp upload vào nghiệp vụ guide

Đã sửa nghiệp vụ tạo guide để dùng upload Cloudinary thật sự:

- Trước: có nhánh lưu local hoặc cơ chế chưa nhất quán.
- Sau: luồng tạo guide gọi trực tiếp Cloudinary service, lấy URL và lưu DB.

Kết quả:

- Dữ liệu guide trong DB chứa URL Cloudinary.
- Không còn lệ thuộc vào local storage cho chức năng guide chính.

## 3.4 Xây dựng endpoint QA cho tài liệu

Đã bổ sung endpoint hỏi đáp nhanh cho module guide:

- API nhận:
  - `activityId`
  - `question`
- API trả:
  - Câu hỏi
  - Câu trả lời AI
  - Danh sách tài liệu tham chiếu

Các thành phần backend bổ sung:

- DTO request cho QA.
- DTO response cho QA.
- Service xử lý nghiệp vụ QA.
- Controller expose endpoint QA.

## 3.5 Trích xuất nội dung PDF bằng PDFBox

Đã tích hợp PDFBox để đọc text từ file PDF:

- Hỗ trợ đọc từ URL guide.
- Chuẩn hóa text (gộp dòng, xử lý khoảng trắng).
- Cắt ngữ cảnh theo ngưỡng để tránh prompt quá dài.
- Giới hạn số lượng tài liệu đưa vào context để tối ưu hiệu năng và chi phí.

Mục tiêu kỹ thuật đạt được:

- AI trả lời dựa trên tài liệu thực tế, không trả lời "chung chung".
- Giữ độ trễ và dung lượng request trong ngưỡng an toàn.

## 3.6 Tích hợp gọi Groq API

Đã hoàn thiện lớp gọi Groq Chat API:

- Dựng payload `messages` theo vai trò system/user.
- Chèn ngữ cảnh tài liệu và câu hỏi người dùng vào prompt.
- Cấu hình model qua env (`GROQ_MODEL`).
- Parse response, lấy `message.content`.
- Xử lý fallback lỗi provider.

Bổ sung kiểm tra điều kiện:

- Thiếu API key -> trả lỗi cấu hình AI chưa sẵn sàng.
- Tài liệu rỗng/không trích xuất được -> trả lỗi nội dung không đủ.
- Provider lỗi -> trả lỗi AI provider.

## 3.7 Bổ sung mã lỗi nghiệp vụ AI

Đã thêm các error code liên quan AI để phân biệt nguyên nhân:

- AI chưa cấu hình.
- Lỗi từ provider.
- Nội dung guide rỗng/không đủ.

Lợi ích:

- Frontend hiển thị thông báo đúng bản chất lỗi.
- Dễ vận hành và debug khi triển khai thật.

## 3.8 Xử lý lỗi xung đột Activity existed

Trong luồng upload/khởi tạo theo tháng đã gặp lỗi `Activity existed`.

Đã sửa logic:

- Nếu hoạt động theo tháng đã tồn tại, không ném lỗi conflict ngay.
- Chuyển sang append guide vào hoạt động hiện có.

Kết quả:

- Người dùng có thể thêm guide vào tháng đã tồn tại mà không bị chặn không cần thiết.

## 3.9 Xử lý sự cố Cloudinary 401 khi mở link PDF

Đây là sự cố trọng điểm trong giai đoạn hoàn thiện thực tế.

Hiện tượng:

- Upload báo thành công.
- Bấm link Cloudinary lại nhận `401 Unauthorized` hoặc viewer báo không tải được PDF.
- QA thất bại do backend không đọc được tài liệu từ link đó.

Quá trình xác minh:

- Kiểm tra trực tiếp URL trả về: nhận 401.
- Đối chiếu metadata tài nguyên qua API quản trị Cloudinary: file tồn tại, access mode public.
- Rà soát Security Settings trên Cloudinary account.

Nguyên nhân vận hành:

- Tài khoản Cloudinary chưa bật quyền delivery cho PDF/ZIP ở phần Security.

Cách khắc phục:

- Bật `Allow delivery of PDF and ZIP files` trong Cloudinary Security.
- Lưu cấu hình và upload lại file mới để test.

Kết quả sau khắc phục:

- Link mở được.
- QA đọc được PDF và trả lời bình thường.

Ghi chú quan trọng:

- Đây là dependency ở tầng hạ tầng dịch vụ (Cloudinary policy), không chỉ là lỗi code.
- Bài học chính: kiểm tra policy delivery song song với kiểm tra mã nguồn.

---

## 4) Các hạng mục frontend đã hoàn thiện

## 4.1 Tích hợp gọi API QA

Đã bổ sung:

- DTO request/response cho QA.
- Method gọi API `askQuestion` trong service guide.
- Luồng state trong component:
  - `qaQuestion`
  - `qaLoading`
  - `qaResult`
  - `qaErrorKey`

Kết quả:

- Người dùng nhập câu hỏi và nhận phản hồi trực tiếp ngay trong cùng màn hình.

## 4.2 Thiết kế panel "Hỏi đáp nhanh"

Đã thêm khu vực UI riêng cho QA gồm:

- Tiêu đề và mô tả tính năng.
- Textarea nhập câu hỏi.
- Nút "Hỏi AI" có trạng thái loading/disable hợp lý.
- Khối hiển thị câu trả lời.
- Khối hiển thị tài liệu tham chiếu.

Cải tiến bố cục theo yêu cầu:

- Bảng danh sách file ở trên.
- Khung chat/QA ở dưới.

## 4.3 Đa ngôn ngữ có dấu

Đã cập nhật i18n cho các chuỗi QA ở nhiều ngôn ngữ:

- Tiếng Việt
- Tiếng Anh
- Tiếng Trung
- Tiếng Lào

Kết quả:

- Nội dung hiển thị đồng nhất với ngôn ngữ giao diện.
- Không còn tình trạng text không dấu ở các vị trí chính.

## 4.4 Cải thiện dark theme

Đã chỉnh màu chữ/nền để tăng tương phản:

- Text dễ đọc hơn ở panel QA.
- Thành phần upload, progress và vùng nội dung đồng bộ theme tối.

## 4.5 Sửa UX thông báo upload thành công bị che

Đã chỉnh vị trí và z-index của toast để:

- Không bị header che.
- Hiển thị rõ ở cả desktop và mobile.

## 4.6 Sửa UX xóa file phải reload mới mất

Đã cập nhật xử lý sau khi xóa:

- Cập nhật danh sách ngay tại chỗ (optimistic/local update).
- Reload đúng luồng theo ngữ cảnh chi tiết/tổng quan.

Kết quả:

- Bấm xóa là thấy file biến mất ngay, không cần F5.

## 4.7 Upload xong tự chuyển về màn hình danh sách + khung chat

Đã bổ sung điều hướng sau upload thành công:

- Upload guide thành công -> quay về màn hình chi tiết activity có bảng file + panel QA.
- Trải nghiệm liền mạch, không bắt người dùng điều hướng thủ công.

---

## 5) Dọn dẹp và chuẩn hóa thành phần AI cũ

Đã loại bỏ các thành phần không còn dùng (ví dụ nhánh Dust cũ) để tránh:

- Trùng lặp logic AI.
- Khó bảo trì.
- Cấu hình dư thừa gây nhầm lẫn.

Kết quả:

- Kiến trúc AI tập trung vào Groq cho use case QA tài liệu.

---

## 6) Kiểm thử và xác nhận kết quả

## 6.1 Kiểm thử kỹ thuật đã thực hiện

- Build backend nhiều vòng sau mỗi thay đổi chính.
- Build frontend sau các thay đổi UI/logic.
- Kiểm tra endpoint docs/health để xác nhận app chạy đúng context path.
- Test upload thật với tài khoản Cloudinary thật.
- Test mở URL PDF thật.
- Test QA end-to-end với tài liệu vừa upload.

## 6.2 Kết quả chức năng đạt được

- Upload PDF lên Cloudinary: hoạt động.
- Lưu URL guide vào DB: hoạt động.
- Mở link guide: hoạt động sau khi bật policy PDF/ZIP delivery.
- Hỏi đáp nhanh Groq AI: hoạt động.
- Xóa file cập nhật ngay UI: hoạt động.
- Upload xong tự quay lại màn hình danh sách + QA: hoạt động.

---

## 7) Cấu hình môi trường cần duy trì

## 7.1 Biến môi trường backend cần có

Cloudinary:

- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Groq:

- `GROQ_BASE_URL` (mặc định có thể dùng `https://api.groq.com`)
- `GROQ_API_KEY`
- `GROQ_MODEL` (ví dụ `llama-3.1-8b-instant`)

## 7.2 Cấu hình Cloudinary Security bắt buộc

- Bật `Allow delivery of PDF and ZIP files`.

Nếu không bật mục này:

- Upload vẫn có thể thành công.
- Nhưng mở file qua link có thể bị 401/không tải được.
- QA sẽ thất bại do không đọc được nội dung PDF từ URL.

## 7.3 Triển khai DigitalOcean

- Auto Deploy có thể đang bật ở App Platform theo nhánh đã kết nối.
- Cần set đầy đủ env vars trong App Settings của DigitalOcean.
- Sau khi đổi key, cần redeploy để app nhận biến mới.

---

## 8) Các vấn đề đã gặp và cách xử lý chi tiết

## 8.1 Vấn đề: Cloudinary chưa dùng thật trong luồng guide

- Dấu hiệu: tính năng nói dùng Cloudinary nhưng đường đi thực tế chưa nhất quán.
- Xử lý: sửa service nghiệp vụ để gọi Cloudinary upload thật.

## 8.2 Vấn đề: `Activity existed` khi thêm sinh hoạt lớp

- Dấu hiệu: thao tác thêm/upload bị chặn conflict.
- Xử lý: đổi logic sang append guide vào activity đã có của tháng.

## 8.3 Vấn đề: Upload thành công nhưng mở PDF lỗi 401

- Dấu hiệu: URL trả về Cloudinary không mở được.
- Xử lý:
  - xác minh metadata tài nguyên,
  - kiểm tra policy Security,
  - bật PDF/ZIP delivery,
  - test lại bằng file upload mới.

## 8.4 Vấn đề: QA không trả lời

- Dấu hiệu: ask question thất bại hoặc rỗng.
- Nguyên nhân phụ thuộc:
  - link tài liệu không truy cập được,
  - thiếu API key,
  - không trích xuất được text.
- Xử lý: lần lượt kiểm tra URL, cấu hình key, log service AI.

## 8.5 Vấn đề UX: xóa phải reload, upload xong đứng nguyên màn hình

- Dấu hiệu: trải nghiệm thao tác chậm/không trực quan.
- Xử lý:
  - update list ngay sau delete,
  - tự điều hướng sau upload thành công.

---

## 9) Giá trị mang lại sau khi hoàn thiện

## 9.1 Giá trị nghiệp vụ

- Quy trình quản lý tài liệu hướng dẫn trở nên tập trung và ổn định hơn.
- Người dùng có thể khai thác tri thức từ tài liệu nhanh hơn nhờ hỏi đáp AI.
- Giảm thao tác thủ công (copy mở file, đọc toàn bộ tài liệu, tự tìm câu trả lời).

## 9.2 Giá trị kỹ thuật

- Tách bạch rõ upload/storage và lớp AI.
- Cấu hình nhạy cảm được chuẩn hóa theo env vars.
- Có bộ error code rõ ràng để vận hành.
- Xử lý được các tình huống thực tế khi tích hợp dịch vụ bên thứ ba.

## 9.3 Giá trị trải nghiệm

- UI rõ ràng hơn, đa ngôn ngữ tốt hơn, dark theme dễ nhìn hơn.
- Luồng thao tác liên tục và phản hồi tức thời.

---

## 10) Checklist nghiệm thu chức năng

Checklist đề xuất để xác nhận đầy đủ:

1. Đăng nhập bằng tài khoản có quyền tạo guide.
2. Vào màn hình thêm hướng dẫn, chọn 1 file PDF hợp lệ.
3. Upload thành công, hệ thống tự quay về màn hình danh sách + panel QA.
4. Bấm link "Click here" mở được tài liệu PDF.
5. Nhập câu hỏi trong panel QA, bấm "Hỏi AI".
6. Hệ thống trả câu trả lời và hiển thị danh sách tài liệu tham chiếu.
7. Bấm xóa một file, file biến mất ngay không cần reload trang.
8. Đổi ngôn ngữ giao diện và xác nhận text QA hiển thị đúng.
9. Bật dark theme và xác nhận độ tương phản chữ.
10. Kiểm tra log backend không có lỗi bất thường trong các bước trên.

---

## 11) Khuyến nghị vận hành và bảo mật

1. Không commit key thật vào mã nguồn hoặc tài liệu công khai.
2. Định kỳ rotate API keys (Cloudinary/Groq).
3. Theo dõi quota và rate limit của Groq + Cloudinary.
4. Bổ sung dashboard log/metrics cho endpoint QA.
5. Cân nhắc cache kết quả trích xuất PDF cho tài liệu ít thay đổi.
6. Có thể thêm proxy download backend nếu muốn giảm phụ thuộc vào viewer bên thứ ba.

---

## 12) Tổng kết

Chức năng upload tài liệu lên Cloudinary và hỏi đáp nhanh bằng Groq AI đã được hoàn thiện end-to-end, bao gồm cả phần kỹ thuật lõi, UI/UX, cấu hình môi trường và xử lý sự cố thực tế trong quá trình chạy thật.

Điểm quan trọng nhất đã xác thực:

- Upload hoạt động thực sự trên Cloudinary.
- Link tài liệu có thể mở được khi policy Cloudinary được cấu hình đúng.
- AI đọc tài liệu và trả lời đúng theo ngữ cảnh.
- Trải nghiệm người dùng đã được cải thiện rõ rệt ở các thao tác chính.

Đây là trạng thái sẵn sàng để đưa vào vận hành demo/prod, với điều kiện duy trì đúng cấu hình môi trường và chính sách bảo mật đã nêu.
