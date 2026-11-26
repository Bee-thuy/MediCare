# Smart Healthcare — Mobile UI cho Admin · Doctor · Patient

Đây là **bản prototype tương tác** được thiết kế theo nội dung tab `UI Admin`, `UI Bác sĩ` và `UI Bệnh nhân` của bảng Google Sheets đồ án tốt nghiệp.

## Nội dung

- **Admin — 8 nhóm:** tổng quan, duyệt yêu cầu lịch, bác sĩ, bệnh nhân, ca làm việc, chuyên khoa, thông báo, báo cáo.
- **Bác sĩ — 8 nhóm:** dashboard, lịch khám, bệnh nhân, kết quả khám, kê đơn, chat, lịch làm việc, hồ sơ.
- **Bệnh nhân — 10 nhóm:** đăng nhập/đăng ký, trang chủ, tìm bác sĩ, đặt lịch, quản lý lịch, chat, hồ sơ sức khỏe, nhắc thuốc, đánh giá, cài đặt.
- Các màn hình con: chi tiết lịch, xử lý trùng 08:30, thay đổi giờ, chi tiết bác sĩ, biểu mẫu thêm/sửa, công bố kết quả, xem đơn thuốc, v.v.

## Cách mở

Giải nén và mở **`index.html`** bằng Chrome, Safari hoặc trình duyệt mới. Không cần máy chủ, tài khoản, thư viện hoặc Internet.

Trên máy tính, giao diện hiển thị trong khung điện thoại. Trên điện thoại, giao diện tự giãn theo chiều rộng màn hình.

## Thử một luồng tích hợp

1. Chọn **Bệnh nhân → Đặt lịch**, chọn bác sĩ, ngày, giờ, lý do và gửi yêu cầu. Trạng thái là **Chờ duyệt**.
2. Chọn **Admin → Yêu cầu đặt lịch**, mở yêu cầu và duyệt. Nếu trùng giờ với lịch đã duyệt, chọn **Đề xuất giờ khác**.
3. Chọn **Bác sĩ → Lịch khám**, lịch đã được duyệt sẽ xuất hiện; mở chi tiết và tạo kết quả khám.
4. Chọn **Bệnh nhân → Hồ sơ sức khỏe**, xem kết quả được bác sĩ công bố và đơn thuốc nếu có.
5. Tại **Bệnh nhân → Thông báo & cài đặt**, đổi tài khoản demo sang **Lê Thị Mai** để thấy yêu cầu trùng giờ của người này, rồi kiểm tra luồng đề xuất đổi giờ.

Bản demo có sẵn **3 yêu cầu cùng 08:30** ngày 17/11/2026 với BS. Trần Minh Đức. Sức chứa thử nghiệm là một người trong khung đó.

## Bản vẽ đi kèm

- `bo_giao_dien_admin.png`: 8 màn hình chính Admin (mobile).
- `bo_giao_dien_doctor.png`: 8 màn hình chính Bác sĩ (mobile).
- `bo_giao_dien_patient.png`: 10 màn hình chính Bệnh nhân (mobile).
- `screenshots/`: ảnh riêng từng màn hình và màn hình con như các bước đặt lịch, biểu mẫu kê đơn, xử lý trùng lịch.

## Lưu ý kỹ thuật

- Đây là **UI/UX prototype HTML, CSS, JavaScript**; **không phải** dự án Android Studio (Java/XML) và chưa có backend, xác thực email, lưu trữ máy chủ hay chat realtime.
- Các nút tương tác và CRUD chỉ thay đổi **dữ liệu minh họa trên trình duyệt** (`localStorage`). Có nút **Đặt lại dữ liệu demo** trong menu chức năng.
- Mẫu chỉ phục vụ phân tích giao diện và diễn giải luồng nghiệp vụ; không sử dụng để khám bệnh thật hoặc kê đơn cho bệnh nhân thật.

Tham chiếu chức năng: https://docs.google.com/spreadsheets/d/1piN6IHIcDSoSndKurhOJJ38_qzB0kU9hE-RsOyfWGwQ/edit
