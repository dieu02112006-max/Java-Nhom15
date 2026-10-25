# Todo List REST API

Ứng dụng Spring Boot quản lý năm bảng trong thiết kế: `task_lists`, `tasks`,
`tags`, `task_tags` và `reminders`. Dữ liệu được lưu trong bộ nhớ; khởi động lại
ứng dụng sẽ xóa dữ liệu. `userId` và `budgetId` được lưu dưới dạng ID vì các bảng
`users` và `budgets` không nằm trong thiết kế được cung cấp.

## Chạy ứng dụng

Yêu cầu Java 21:

```powershell
.\mvnw.cmd spring-boot:run
```

## REST API

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