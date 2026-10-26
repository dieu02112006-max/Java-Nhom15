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
mẫu `userId=1`, vì vậy database cần có người dùng này cùng ví và danh mục.

Các giao diện:

| Giao diện | Đường dẫn |
| --- | --- |
| Trang chủ | `/` |
| To-do list | `/todo/index.html` |
| Quản lí chi tiêu | `/expenses/index.html` |

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