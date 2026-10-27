# Dayflow — To-do list và quản lí chi tiêu

Ứng dụng Spring Boot cung cấp giao diện To-do list và quản lí chi tiêu. Dữ liệu
To-do hiện được lưu trong bộ nhớ; giao dịch tài chính được đọc và ghi trong
MySQL qua các bảng `transactions`, `categories`, `wallets` và `budgets`.

## Chạy ứng dụng

Yêu cầu Java 21:

```powershell
.\mvnw.cmd spring-boot:run
```

Để dùng giao diện quản lí chi tiêu, khởi động MySQL, tạo database `java_nhom15`
và nạp cấu trúc/dữ liệu từ `java_nhom15.sql`. Cấu hình kết nối nằm trong
`src/main/resources/application.properties`. Giao diện mặc định dùng tài khoản
mẫu để hiển thị dữ liệu chi tiêu, vì vậy database cần có người dùng cùng ví và danh mục.

## Đăng nhập và phân quyền

Ứng dụng dùng phiên đăng nhập trên server, mật khẩu BCrypt, CSRF token cho các
thao tác ghi và hai vai trò `USER` / `ADMIN`. Người đăng ký mới luôn là `USER`;
chỉ `ADMIN` được quản lí tài khoản và truy cập trang quản trị. Tác vụ To-do và
chi tiêu được giới hạn theo tài khoản đang đăng nhập.

Nếu database đã được tạo từ phiên bản trước, chạy một lần migration sau trước
khi khởi động ứng dụng:

```powershell
Get-Content -Raw database/migrations/002-add-user-role.sql | mysql -u root java_nhom15
```

Tạo Admin đầu tiên bằng email và mật khẩu riêng của bạn trong PowerShell, rồi
khởi động ứng dụng. Mật khẩu cần ít nhất 12 ký tự; ứng dụng chỉ tạo tài khoản
nếu email đó chưa tồn tại.

```powershell
$env:DAYFLOW_ADMIN_EMAIL = "admin@example.com"
$env:DAYFLOW_ADMIN_PASSWORD = "thay-bang-mat-khau-rieng-12-ky-tu"
```

Đăng nhập tại `/login/index.html`. Người dùng thường có thể tự tạo tài khoản ở
trang này. Admin vào `/admin/index.html`; trang quản trị cho phép đổi vai trò
người dùng. Không thể tự hạ quyền/xóa tài khoản Admin đang đăng nhập hoặc xóa
quản trị viên cuối cùng.

API xác thực: `POST /api/auth/register`, `POST /api/auth/login`,
`GET /api/auth/me` và `POST /api/auth/logout`. Các request ghi cần gửi CSRF token
trong header `X-XSRF-TOKEN`; lấy token trước bằng `GET /api/auth/csrf`.

Các giao diện:

| Giao diện | Đường dẫn |
| --- | --- |
| Trang chủ | `/` |
| To-do list | `/todo/index.html` |
| Quản lí chi tiêu | `/expenses/index.html` |
| Mục tiêu tiết kiệm | `/expenses/goals.html` |
| Đăng nhập / đăng ký | `/login/index.html` |
| Quản trị | `/admin/index.html` (chỉ ADMIN) |

## REST API

### To-do list

Dữ liệu của các tài nguyên To-do được lưu trong bộ nhớ; khởi động lại ứng dụng
sẽ xóa dữ liệu.

| Tài nguyên | Đường dẫn | Chức năng |
| --- | --- | --- |
| Task lists | `/api/task-lists` | GET, POST; `/{id}` hỗ trợ GET, PUT, DELETE |
| Tasks | `/api/tasks` | GET, POST; `/{id}` hỗ trợ GET, PUT, DELETE |
| Tags | `/api/tags` | GET, POST; `/{id}` hỗ trợ GET, PUT, DELETE |
| Task tags | `/api/task-tags` | GET, POST; `/{taskId}/{tagId}` hỗ trợ GET, DELETE |
| Reminders | `/api/reminders` | GET, POST; `/{id}` hỗ trợ GET, PUT, DELETE |

`GET /api/tasks/{id}` trả về tài nguyên có liên kết HATEOAS. Các trường ngày giờ
dùng định dạng ISO-8601, ví dụ `2026-10-25T14:30:00`. Trạng thái task nhận
`todo`, `in_progress`, `done`; độ ưu tiên nhận `low`, `medium`, `high`.

### Quản lí chi tiêu

Các endpoint dưới đây dùng `userId=1` nếu không truyền `userId`. Tháng dùng định
dạng `YYYY-MM` và mặc định là tháng hiện tại.

| Đường dẫn | Chức năng |
| --- | --- |
| `GET /api/finance/summary?userId=1&month=2026-10` | Tổng thu, chi, số dư ví và ngân sách |
| `GET /api/finance/transactions?userId=1&month=2026-10` | Danh sách giao dịch trong tháng |
| `GET /api/finance/categories?userId=1&type=expense` | Danh mục thu hoặc chi |
| `GET /api/finance/wallets?userId=1` | Ví đang hoạt động của người dùng |
| `POST /api/finance/transactions` | Thêm giao dịch và cập nhật số dư ví |
| `DELETE /api/finance/transactions/{id}?userId=1` | Xóa mềm giao dịch và hoàn số dư ví |

Giao dịch mới nhận JSON gồm `userId`, `walletId`, `categoryId` (có thể `null`),
`type` (`income` hoặc `expense`), `amount`, `transactionDate` (ISO-8601) và
`note`. Mỗi giao dịch phải thuộc một ví đang hoạt động của người dùng.
