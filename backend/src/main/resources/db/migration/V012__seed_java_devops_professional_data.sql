-- V012: Bộ dữ liệu chuyên sâu Java Backend & DevOps
-- Idempotent: có thể chạy lại mà không nhân bản dữ liệu.

ALTER TABLE questions ADD COLUMN IF NOT EXISTS follow_up_question TEXT;

COMMENT ON COLUMN questions.follow_up_question IS 'Câu hỏi đào sâu gợi ý cho lượt phỏng vấn tiếp theo.';

-- ================================================================
-- 1. Chọn topic Java hiện có và bổ sung alias theo ngôn ngữ thực tế
-- ================================================================
INSERT INTO topic_aliases (topic_id, alias)
VALUES
('11111111-1111-1111-1111-111111111111', 'Encapsulation'),
('11111111-1111-1111-1111-111111111111', 'Inheritance'),
('11111111-1111-1111-1111-111111111111', 'Polymorphism'),
('11111111-1111-1111-1111-111111111111', 'Abstraction'),
('11111111-1111-1111-1111-111111111111', 'SOLID'),
('22222222-2222-2222-2222-222222222222', 'Java Core'),
('22222222-2222-2222-2222-222222222222', 'Exception Handling'),
('22222222-2222-2222-2222-222222222222', 'Generics'),
('22222222-2222-2222-2222-222222222222', 'Reflection'),
('22222222-2222-2222-2222-222222222222', 'String Pool'),
('22222222-2222-2222-2222-222222222222', 'Immutability'),
('33333333-3333-3333-3333-333333333333', 'ConcurrentHashMap'),
('33333333-3333-3333-3333-333333333333', 'Comparable'),
('44444444-4444-4444-4444-444444444444', 'Lambda Expression'),
('44444444-4444-4444-4444-444444444444', 'CompletableFuture'),
('55555555-5555-5555-5555-555555555555', 'ThreadPool'),
('55555555-5555-5555-5555-555555555555', 'ExecutorService'),
('55555555-5555-5555-5555-555555555555', 'Synchronized'),
('55555555-5555-5555-5555-555555555555', 'Volatile'),
('55555555-5555-5555-5555-555555555555', 'Deadlock'),
('66666666-6666-6666-6666-666666666666', 'Stack'),
('66666666-6666-6666-6666-666666666666', 'G1GC'),
('66666666-6666-6666-6666-666666666666', 'Memory Leak'),
('77777777-7777-7777-7777-777777777777', 'IoC'),
('77777777-7777-7777-7777-777777777777', 'RESTful API'),
('77777777-7777-7777-7777-777777777777', 'Spring MVC'),
('88888888-8888-8888-8888-888888888888', 'Spring Data JPA'),
('88888888-8888-8888-8888-888888888888', 'N+1 Query'),
('88888888-8888-8888-8888-888888888888', 'Transaction'),
('88888888-8888-8888-8888-888888888888', 'Indexing'),
('88888888-8888-8888-8888-888888888888', 'ACID')
ON CONFLICT (topic_id, alias) DO NOTHING;

-- ================================================================
-- 2. Sáu chủ đề DevOps trọng tâm
-- ================================================================
INSERT INTO topics (id, code, parent_id, is_active) VALUES
('a1000000-0000-0000-0000-000000000001','linux_networking',NULL,true),
('a1000000-0000-0000-0000-000000000002','docker',NULL,true),
('a1000000-0000-0000-0000-000000000003','kubernetes',NULL,true),
('a1000000-0000-0000-0000-000000000004','cicd_pipelines',NULL,true),
('a1000000-0000-0000-0000-000000000005','monitoring_observability',NULL,true),
('a1000000-0000-0000-0000-000000000006','microservices_architecture',NULL,true)
ON CONFLICT (id) DO UPDATE SET code=EXCLUDED.code, parent_id=EXCLUDED.parent_id, is_active=EXCLUDED.is_active;

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
('a1000000-0000-0000-0000-000000000001','vi','Linux & Networking cơ bản','Linux CLI, Bash, SSH, DNS, HTTP/HTTPS và Reverse Proxy trong vận hành hệ thống.'),
('a1000000-0000-0000-0000-000000000001','en','Linux & Networking Basics','Linux CLI, Bash, SSH, DNS, HTTP/HTTPS and reverse proxy operations.'),
('a1000000-0000-0000-0000-000000000002','vi','Docker & Containerization','Dockerfile, Docker Compose, image tối ưu, container lifecycle và networking.'),
('a1000000-0000-0000-0000-000000000002','en','Docker & Containerization','Dockerfile, Docker Compose, image optimization, lifecycle and container networking.'),
('a1000000-0000-0000-0000-000000000003','vi','Kubernetes (K8s)','Pod, Deployment, Service, Ingress, scheduling và troubleshooting workload.'),
('a1000000-0000-0000-0000-000000000003','en','Kubernetes (K8s)','Pod, Deployment, Service, Ingress, scheduling and workload troubleshooting.'),
('a1000000-0000-0000-0000-000000000004','vi','CI/CD Pipelines','Build, test, security scan, artifact và deployment automation.'),
('a1000000-0000-0000-0000-000000000004','en','CI/CD Pipelines','Build, test, security scan, artifact and deployment automation.'),
('a1000000-0000-0000-0000-000000000005','vi','Monitoring & Logging','Metrics, logs, traces, Prometheus, Grafana, ELK và alerting.'),
('a1000000-0000-0000-0000-000000000005','en','Monitoring & Logging','Metrics, logs, traces, Prometheus, Grafana, ELK and alerting.'),
('a1000000-0000-0000-0000-000000000006','vi','Microservices & Cloud cơ bản','Microservices, API Gateway, gRPC, message broker và nền tảng cloud.'),
('a1000000-0000-0000-0000-000000000006','en','Microservices & Cloud Basics','Microservices, API Gateway, gRPC, message brokers and cloud fundamentals.')
ON CONFLICT (topic_id, language) DO UPDATE SET name=EXCLUDED.name, description=EXCLUDED.description;

INSERT INTO topic_aliases (topic_id, alias) VALUES
('a1000000-0000-0000-0000-000000000001','Linux CLI'),('a1000000-0000-0000-0000-000000000001','Bash Script'),('a1000000-0000-0000-0000-000000000001','SSH'),('a1000000-0000-0000-0000-000000000001','DNS'),('a1000000-0000-0000-0000-000000000001','HTTP/HTTPS'),('a1000000-0000-0000-0000-000000000001','Reverse Proxy'),
('a1000000-0000-0000-0000-000000000002','Docker'),('a1000000-0000-0000-0000-000000000002','Dockerfile'),('a1000000-0000-0000-0000-000000000002','Docker Compose'),('a1000000-0000-0000-0000-000000000002','Multi-stage Build'),('a1000000-0000-0000-0000-000000000002','Container'),('a1000000-0000-0000-0000-000000000002','Docker Network'),
('a1000000-0000-0000-0000-000000000003','Kubernetes'),('a1000000-0000-0000-0000-000000000003','K8s'),('a1000000-0000-0000-0000-000000000003','Pod'),('a1000000-0000-0000-0000-000000000003','Deployment'),('a1000000-0000-0000-0000-000000000003','Service'),('a1000000-0000-0000-0000-000000000003','Ingress'),
('a1000000-0000-0000-0000-000000000004','CI/CD'),('a1000000-0000-0000-0000-000000000004','GitLab CI'),('a1000000-0000-0000-0000-000000000004','GitHub Actions'),('a1000000-0000-0000-0000-000000000004','Jenkins'),('a1000000-0000-0000-0000-000000000004','Automated Testing'),('a1000000-0000-0000-0000-000000000004','Deployment Pipeline'),
('a1000000-0000-0000-0000-000000000005','Prometheus'),('a1000000-0000-0000-0000-000000000005','Grafana'),('a1000000-0000-0000-0000-000000000005','Actuator'),('a1000000-0000-0000-0000-000000000005','ELK Stack'),('a1000000-0000-0000-0000-000000000005','Alertmanager'),('a1000000-0000-0000-0000-000000000005','Observability'),
('a1000000-0000-0000-0000-000000000006','Microservices'),('a1000000-0000-0000-0000-000000000006','gRPC'),('a1000000-0000-0000-0000-000000000006','Protobuf'),('a1000000-0000-0000-0000-000000000006','API Gateway'),('a1000000-0000-0000-0000-000000000006','Message Broker'),('a1000000-0000-0000-0000-000000000006','AWS cơ bản')
ON CONFLICT (topic_id, alias) DO NOTHING;

-- ================================================================
-- 3. 14 tài liệu ôn tập đã đánh chỉ mục
-- ================================================================
INSERT INTO documents (id, topic_id, language, title, content_md, status, index_error)
VALUES
('d2000000-0000-0000-0000-000000000001','11111111-1111-1111-1111-111111111111','vi','SOLID và thiết kế hướng đối tượng thực chiến', $$# SOLID trong Java Backend

## Tổng quan
SOLID là tập nguyên tắc giúp hệ thống OOP dễ mở rộng, kiểm thử và duy trì. Không phải là design pattern và không bắt buộc áp dụng máy móc; đánh đổi cần cân nhắc theo quy mô dịch vụ.

## Nội dung dưới lớp
- **SRP:** một class có một trách nhiệm thay đổi rõ ràng.
- **OCP:** mở rộng bằng abstraction thay vì thay đổi nhánh logic đã ổn định.
- **LSP:** subtype phải thay thế được base type mà không phá hợp đồng.
- **ISP:** interface nhỏ, đúng vai trò thay vì interface khổng lồ.
- **DIP:** module cấp cao phụ thuộc abstraction, không phụ thuộc implementation.

```java
interface PaymentGateway { PaymentResult pay(Money amount); }

class PaymentService {
    private final PaymentGateway gateway;
    PaymentService(PaymentGateway gateway) { this.gateway = gateway; }
}
```

## So sánh
| Cách làm | Ưu điểm | Khi nào dùng |
|---|---|---|
| Interface Segregation | Giảm phụ thuộc, dễ mock | Dịch vụ có nhiều implementation |
| Fat interface | Viết ít class ban đầu | Prototype nhỏ nhưng rủi ro lớn khi mở rộng |

## Lỗi thường gặp
- Tạo interface cho từng class chỉ phục vụ một implementation.
- Áp dụng SOLID làm code phân mảnh nhưng không giải quyết domain cohesion.
- Thêm abstraction khi chưa có hành vi thay đổi thực tế.

## Best Practices
Ưu tiên domain rõ ràng, constructor injection, test thay interface và kiểm tra mức coupling bằng code review.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000002','22222222-2222-2222-2222-222222222222','vi','Core Java: Generics, Immutability và Exception Handling', $$# Core Java cho Backend Developer

## Tổng quan
Một ứng viên Middle cần giải thích được hành vi runtime của generics, object immutability và chiến lược xử lý exception, không chỉ thuộc các từ khóa.

## Generics và type erasure
Generics kiểm tra kiểu tại compile time và bị xóa kiểu ở bytecode. Vì vậy không thể tạo `T[]` trực tiếp hoặc overload cùng signature sau erasure.

```java
public final class Money {
    private final long cents;
    private final Currency currency;

    public Money(long cents, Currency currency) {
        this.cents = cents;
        this.currency = currency;
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException();
        return new Money(cents + other.cents, currency);
    }
}
```

## Exception
- `checked exception` phù hợp với tình huống caller có khả năng khôi phục.
- `runtime exception` phù hợp với lập trình sai hoặc điều kiện nghiệp vụ bất biến.
- Không bắt exception cấp thấp chỉ để ghi log rồi tiếp tục như bình thường.

## So sánh
| Khái niệm | Đặc điểm | Lưu ý |
|---|---|---|
| String Pool | Tái sử dụng literal | Không dựa vào `==` để so sánh nội dung |
| Defensive copy | Sao chép dữ liệu sở hữu ngoài | Cần khi trả bộ sưu tập từ DTO/domain |
| Immutable | Trạng thái không đổi sau khởi tạo | Giúp concurrency đơn giản hơn |

## Lỗi thường gặp
- Catch `Exception` quá rộng.
- Biến mutable được expose qua getter/setter.
- So sánh String bằng `==`.
- Dùng `e.printStackTrace()` thay vì logging có MDC/request ID.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000003','33333333-3333-3333-3333-333333333333','vi','Collections Framework: Chọn cấu trúc dữ liệu đúng', $$# Java Collections trong hệ thống Backend

## Tổng quan
Collections Framework hỗ trợ chọn cấu trúc theo truy cập, thứ tự, tính trùng lặp và yêu cầu concurrency. Độ phức tạp trung bình thường quan trọng hơn kích thước code.

## So sánh lựa chọn
| Nhu cầu | Cấu trúc | Độ phức tạp điển hình |
|---|---|---|
| Truy cập index | ArrayList | get O(1), insert middle O(n) |
| Map key-value | HashMap | get/put O(1) trung bình |
| Map concurrent | ConcurrentHashMap | đọc ghi concurrent, không đảm bảo weakly consistent iteration |
| Thu thập duy nhất có thứ tự | LinkedHashSet | add/contains O(1) trung bình |

```java
Map<Currency, Long> totals = new HashMap<>();
for (Payment p : payments) {
    totals.merge(p.currency(), p.amount(), Long::sum);
}
```

## Comparable và Comparator
- `Comparable` định nghĩa thứ tự tự nhiên trong entity.
- `Comparator` định nghĩa thứ tự ngữ cảnh ngoài entity.
- Luôn nhất quán `compareTo` với `equals` khi dùng trong SortedSet/SortedMap.

## Under the hood
HashMap dùng hash bucket, xử lý collision bằng node/tree; số bucket được resize theo load factor. Hash key phải stable và immutable.

## Lỗi thực tế
- Dùng mutable object làm key.
- Sử dụng `HashMap` trong luồng concurrent rồi thấy dữ liệu lệch.
- Copy toàn bộ collection trong mỗi request thay vì streaming/lazy projection.
- Từ khóa hóa hai class String và custom value cùng hash rồi mất key.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000004','44444444-4444-4444-4444-444444444444','vi','Lambda, Stream và CompletableFuture cho xử lý bất đồng bộ', $$# Java 8+ cho dịch vụ thực tế

## Lambda và Functional Interface
Lambda không thay thế object hoàn toàn mà là cách truyền hành vi. Functional interface có một abstract method nhưng có thể có default method.

```java
List<String> activeEmails = users.stream()
    .filter(User::isActive)
    .map(User::getEmail)
    .distinct()
    .sorted()
    .toList();
```

## Optional
Optional dùng làm giá trị trả về, không dùng cho field hoặc parameter. Không gọi `get()` trước `isPresent()`.

```java
String email = repository.findById(id)
    .map(User::getEmail)
    .orElseThrow(() -> new ResourceNotFoundException(id));
```

## CompletableFuture
Ưu tiên khi cần ghép nhiều tác vụ I/O độc lập. Luôn chọn executor phù hợp thay vì common pool cho workload blocking.

```java
CompletableFuture<User> user = supplyAsync(this::loadUser, executor);
CompletableFuture<Plan> plan = supplyAsync(this::loadPlan, executor);
return user.thenCombine(plan, Response::new).join();
```

## So sánh
| Công cụ | Nên dùng | Tránh |
|---|---|---|
| Stream | Transform pipeline | Side effect trong terminal loop |
| Optional | Return value có thể thiếu | Serialize Optional trong DTO |
| CompletableFuture | Ghép tác vụ async | `.join()` trong request thread không kiểm soát timeout |

## Lỗi thường gặp
- Parallel stream cho I/O và làm nghẽn common pool.
- Không xử lý timeout/cancellation.
- Xử lý exception trong thenApply nhưng quên thenCompose/exceptionally.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000005','55555555-5555-5555-5555-555555555555','vi','Concurrency: Executor, Locks, Deadlock và Thread Safety', $$# Concurrency cho Java Backend

## Tổng quan
Backend concurrency khác chạy một thread demo: cần kiểm soát pool size, queue, timeout, visibility và backpressure.

## ExecutorService
Nên dùng ThreadPoolExecutor có core/max size, bounded queue, rejected policy và thread naming cụ thể.

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    8, 16, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(200),
    r -> new Thread(r, "billing-worker-" + r.hashCode()),
    new ThreadPoolExecutor.AbortPolicy());
```

## Dấu hiệu thread safety
- `synchronized` bảo vệ monitor và đảm bảo mutual exclusion/visibility.
- `volatile` chỉ đảm bảo visibility, không thay phiên cho compound operation.
- `Atomic*` phù hợp một biến trạng thái độc lập.
- Lock nhiều tài nguyên cần thứ tự thống nhất để tránh deadlock.

## Deadlock
Cần có mutual exclusion, hold-and-wait, no preemption và circular wait. Hướng xử lý:
1. Lock theo thứ tự toàn cục.
2. Dùng timeout với TryLock.
3. Giữ critical section ngắn.
4. Thu thập thread dump và tìm lock cycle.

## So sánh
| Công cụ | Trường hợp | Tránh |
|---|---|---|
| synchronized | Dữ liệu nhỏ, một monitor | Chia sẻ lock ngoài object không rõ |
| ReentrantLock | Need tryLock/fairness | Quên unlock trong finally |
| ConcurrentHashMap | Shared map | Có atomic compound operation từ nhiều key |

## Lỗi thực tế
- Unbounded queue làm memory bloat.
- New thread mỗi request.
- Đợi futures không giới hạn.
- Dùng volatile cho `count++`.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000006','66666666-6666-6666-6666-666666666666','vi','JVM, Garbage Collection và quy trình debug Memory Leak', $$# JVM & Memory Management

## Tổng quan
JVM gồm class loader, runtime data areas, execution engine và garbage collector. Heap chứa object; stack chứa frame, local variable reference và operand stack theo từng thread.

## Garbage Collection
GC theo dõi reachability thay vì ai tạo object. G1GC chia heap thành region và ưu tiên thu gom region có nhiều garbage.

```bash
java -Xms1g -Xmx1g -XX:+UseG1GC \
  -Xlog:gc*,gc+heap=debug:file=/var/log/app-gc.log:time,uptime \
  -jar app.jar
```

## Quy trình memory leak
1. Thu thập heap dump khi tiêu thụ heap tăng.
2. So sánh object count theo class và dominator tree.
3. Tìm GC root giữ object.
4. Kiểm tra static cache, listener, thread pool, hikari connection.
5. Khắc phục và chạy load test đủ dài.

## Heap vs Stack
| Heap | Stack |
|---|---|
| Object và array | Frame theo từng thread |
| Quản lý bởi GC | Giải phóng khi frame kết thúc |
| Kích thước lớn | Có thể StackOverflow |

## Lỗi thường gặp
- Dùng `-Xmx` quá cao/không bám container memory limit.
- Xem GC log mà không tính throughput và pause.
- Dùng WeakReference để che giấu cache không có eviction policy.
- So sánh native memory bằng heap dump khi vấn đề nằm ở off-heap/metaspace.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000007','77777777-7777-7777-7777-777777777777','vi','Spring Boot: Dependency Injection, REST và cấu hình production', $$# Spring Boot Backend thực chiến

## Tổng quan
Spring Boot tự cấu hình dựa trên classpath và `@ConfigurationProperties`, đồng thời vẫn cho phép override bean/config rõ ràng.

## Dependency Injection
Constructor injection phù hợp dependency bắt buộc vì class có thể khởi tạo không thiếu field.

```java
@Service
public class OrderService {
    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderService(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }
}
```

## REST contract
- Dùng HTTP method và status đúng ngữ nghĩa.
- Validate tại boundary bằng Bean Validation.
- Không trả entity JPA trực tiếp; dùng DTO/view.
- Đặt correlation ID, error code nhất quán.

```yaml
server:
  port: 8080
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

## Under the hood
Spring IoC tạo bean graph, apply AOP proxy và inject dependency. Proxy/self-invocation có thể bỏ qua interceptor như `@Transactional`.

## Lỗi thường gặp
- Field injection làm test/kích thước constructor khó kiểm soát.
- Kích hoạt `@Transactional` trong cùng bean nhưng self-call bỏ qua proxy.
- Bật Actuator toàn bộ endpoint mà không bảo vệ production.
- Catch exception ở service rồi trả success không đúng nghiệp vụ.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000008','88888888-8888-8888-8888-888888888888','vi','JPA/Hibernate: N+1, Transaction và tối ưu truy vấn', $$# Database & JPA cho ứng dụng Backend

## Tổng quan
JPA che SQL nhưng persistence context, flush, lazy loading và transaction boundary vẫn ảnh hưởng trực tiếp query và nhất quán dữ liệu.

## N+1 Query
Tình huống phổ biến: 1 query lấy danh sách + N query lấy association.

```java
@Query("select o from Order o join fetch o.customer where o.status = :status")
List<Order> findActiveWithCustomer(@Param("status") OrderStatus status);
```

## Transaction
`@Transactional` mở transaction và persistence context. Giữ read/write transaction ngắn; không gọi external I/O trong transaction dài.

```java
@Transactional
public void transfer(TransferCommand command) {
    Account debit = repository.findByIdForUpdate(command.debitId());
    Account credit = repository.findByIdForUpdate(command.creditId());
    debit.withdraw(command.amount());
    credit.deposit(command.amount());
}
```

## Indexing
Index tốt khi hỗ trợ predicate JOIN/WHERE/ORDER. Không thêm index cho mọi cột vì ghi chậm và tài nguyên tăng.

| Vấn đề | Dấu hiệu | Hướng xử lý |
|---|---|---|
| N+1 | N+1 SQL trong log | Fetch join/entity graph/projection |
| LazyInitialization | Access ngoài transaction | Fetch đúng ranh giới hoặc DTO query |
| Deadlock DB | Lock wait | Thứ tự lock và retry có giới hạn |

## Lỗi thường gặp
- `EAGER` cho association không cần.
- Mở transaction quanh HTTP call.
- Lấy entity rồi map thủ công khiến lazy loading phình to.
- Không đo query bằng SQL count và execution plan.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-000000000009','a1000000-0000-0000-0000-000000000001','vi','Linux và Networking: Debug SSH, DNS, HTTP và Reverse Proxy', $$# Linux & Networking cơ bản cho DevOps

## Quy trình debug
Bắt đầu từ ứng dụng, sau đó kiểm tra process, port, DNS, routing, firewall và reverse proxy.

```bash
ps aux | grep java
ss -ltnp | grep 8080
curl -v http://127.0.0.1:8080/actuator/health
getent hosts api.internal.local
ip route
```

## DNS và HTTP
DNS phân giải tên thành IP; HTTP/HTTPS phụ thuộc TCP/TLS. Reverse proxy như Nginx kết thúc TLS hoặc chuyển request theo upstream.

```nginx
server {
    listen 443 ssl;
    location / {
        proxy_pass http://app-backend;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    upstream app-backend { server 10.0.1.10:8080; }
}
```

## So sánh
| Lệnh | Mục đích |
|---|---|
| ss | Socket đang listen/established |
| curl -v | Header, TLS, redirect, status |
| dig/getent | DNS resolution |
| iptables/nft | Packet filtering |

## Lỗi thường gặp
- Public port mở nhưng service bind 127.0.0.1.
- DNS cache sai TTL hoặc `/etc/hosts` override.
- Proxy không forward `Host`/`X-Forwarded-*`.
- SSH key permission không đúng hoặc tài khoản chưa có quyền sudo.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-00000000000a','a1000000-0000-0000-0000-000000000002','vi','Docker cho Java: Dockerfile an toàn và tối ưu multi-stage', $$# Docker & Containerization cho Java Backend

## Tổng quan
Docker image cần nhỏ, không chứa secret, có khả năng reproducible và khởi động nhanh. Tách build stage khỏi runtime stage là thực hành chuẩn.

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app/app.jar"]
```

## Under the hood
Một container dùng namespace và cgroup. Docker image là lớp read-only; container tạo writable layer trên cùng.

## Docker Compose
Compose phù hợp môi trường phát triển, không thay thế Kubernetes cho production scale.

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: dev
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: dev
```

## So sánh
| Cách làm | Ưu | Nhược |
|---|---|---|
| Single-stage JDK image | Đơn giản | Image lớn, build tool ở production |
| Multi-stage | Nhỏ, giảm attack surface | Cần quản lý cache tốt |

## Lỗi thường gặp
- COPY `target` trước khi build cache.
- Chạy container bằng root.
- Hard-code DB password trong Dockerfile.
- Không đặt memory limit dù dùng container support.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-00000000000b','a1000000-0000-0000-0000-000000000003','vi','Kubernetes: Deployment, Service, Ingress và CrashLoopBackOff', $$# Kubernetes vận hành thực tế

## Tổng quan
Deployment quản lý Pod replicas, Service cung cấp ổn định network, Ingress điều hướng HTTP bên ngoài và ConfigMap/Secret cung cấp cấu hình.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: java-api }
spec:
  replicas: 2
  selector:
    matchLabels: { app: java-api }
  template:
    metadata:
      labels: { app: java-api }
    spec:
      containers:
        - name: java-api
          image: registry/java-api:1.0.0
          ports: [{ containerPort: 8080 }]
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
```

## Debug CrashLoopBackOff
1. `kubectl get pods`
2. `kubectl describe pod <pod>`
3. `kubectl logs <pod> --previous`
4. Kiểm tra OOMKilled, probe, thiếu secret/config, migration DB hoặc image pull.

## Service và Ingress
| Thành phần | Vai trò |
|---|---|
| ClusterIP Service | Internal load balancing |
| Ingress | HTTP routing và TLS termination |
| Headless Service | Service discovery trực tiếp Pod |

## Lỗi thường gặp
- readiness chưa sẵn sàng trong lúc startup lâu.
- Resources limit làm JVM OOM/CPU throttle.
- Secret chỉ được mount nhưng app không reload.
- Chỉ dùng `latest` tag làm rollout không xác định được artifact.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-00000000000c','a1000000-0000-0000-0000-000000000004','vi','CI/CD Pipeline an toàn cho Spring Boot', $$# CI/CD cho Java Backend

## Tổng quan
Pipeline cần tạo một artifact xác định, chạy test, quét security, publish artifact và deploy có rollback.

```yaml
stages: [test, build, deploy]
variables:
  IMAGE: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA

unit-test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test

build-image:
  stage: build
  script:
    - docker build -t $IMAGE .
    - docker push $IMAGE

deploy-staging:
  stage: deploy
  script:
    - kubectl set image deployment/java-api java-api=$IMAGE
    - kubectl rollout status deployment/java-api --timeout=180s
```

## Nội dung kiểm tra
- Compile và unit test.
- Integration test với Testcontainers/H2 theo rủi ro.
- Dependency/SAST/container scan.
- Build provenance và immutable tag.
- Smoke test sau deploy.

## So sánh
| Chiến lược | Ưu điểm | Rủi ro |
|---|---|---|
| Blue/Green | Rollback nhanh | Tài nguyên kép |
| Canary | Giảm blast radius | Cần observability |
| Rolling | Đơn giản | Không hỗ trợ thay đổi contract tốt |

## Lỗi thường gặp
- Deploy khác artifact đã test.
- Pipeline có secret log ra console.
- Không có rollout guard hoặc smoke test.
- Jenkins/GitHub runner chạy privileged mà không cần.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-00000000000d','a1000000-0000-0000-0000-000000000005','vi','Observability: Metrics, Logs, Traces và Alert rule', $$# Monitoring & Logging

## Ba trụ cột
- Metrics: giá trị theo thời gian để cảnh báo và trend.
- Logs: event chi tiết để điều tra nguyên nhân.
- Traces: luồng request xuyên microservices.

## Spring Boot + Prometheus

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

Prometheus scrape endpoint `/actuator/prometheus`; Grafana truy vấn PromQL. Đừng tạo high-cardinality label theo `userId` hoặc request ID trong metric.

## Alert có hành động

```yaml
- alert: ApiP95LatencyHigh
  expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
  for: 5m
  labels: { severity: warning }
```

## Structured logging
Ghi request ID, tenant ID, operation và duration. Bảo vệ PII/password/token.

## So sánh
| Công cụ | Vai trò |
|---|---|
| Prometheus | Scrape/store metrics |
| Grafana | Dashboard và alert visualization |
| ELK/OpenSearch | Centralized log search |
| Actuator | Spring runtime health/metrics |

## Lỗi thường gặp
- Alert kích hoạt nhưng không có runbook.
- Dashboard chỉ trực quan, không liên kết SLO.
- Log theo level INFO mọi request làm chi phí lớn.
- Chỉ theo CPU/memory mà bỏ độ trễ và tỷ lệ lỗi.$$,'INDEXED',NULL),

('d2000000-0000-0000-0000-00000000000e','a1000000-0000-0000-0000-000000000006','vi','Microservices: API Gateway, Message Broker và resilience', $$# Microservices & Cloud cơ bản

## Tổng quan
Microservices chỉ nên được chọn khi module boundary, ownership và scale/deployment có nhu cầu rõ ràng. Distributed system tăng độ phức tạp về giao tiếp, nhất quán và observability.

## Giao tiếp đồng bộ
REST hoặc gRPC. gRPC tốt cho service-to-service contract hiệu năng cao, nhưng cần quản lý schema/compatibility của Protobuf.

```proto
service OrderService {
  rpc GetOrder(GetOrderRequest) returns (OrderResponse);
}
```

## Giao tiếp bất đồng bộ
Message broker phù hợp khi cần decoupling và xử lý retry/dlq. Bắt buộc idempotency consumer và outbox pattern để tránh mất event.

## Resilience
- Timeout ngắn, bounded retry.
- Circuit breaker theo downstream dependency.
- Bulkhead/cap thread pool.
- Cache hoặc fallback chỉ khi có giá trị nghiệp vụ.

## API Gateway
Gateway tập trung authentication, rate limiting, routing và TLS, nhưng không nên chứa business logic.

## So sánh
| Cách | Khi dùng | Rủi ro |
|---|---|---|
| REST | Public API phổ biến | Chatty calls |
| gRPC | Internal low-latency RPC | Tooling/human readability |
| Message broker | Event-driven async | Ordering, duplicate, delivery semantics |

## Lỗi thường gặp
- Distributed monolith.
- Retry mà không backoff/idempotency.
- Saga không có compensation/correlation ID.
- Mỗi team triển khai observability khác nhau.$$,'INDEXED',NULL)
ON CONFLICT (id) DO UPDATE SET
    topic_id=EXCLUDED.topic_id,
    language=EXCLUDED.language,
    title=EXCLUDED.title,
    content_md=EXCLUDED.content_md,
    status=EXCLUDED.status,
    index_error=EXCLUDED.index_error,
    updated_at=CURRENT_TIMESTAMP;

-- ================================================================
-- 4. 42 câu hỏi phỏng vấn: 3 cấp độ cho 14 chủ đề
-- Schema mức độ: Fresher=JUNIOR, Junior=MID, Middle=SENIOR
-- ================================================================
INSERT INTO questions (id, topic_id, language, level, content, reference_answer, status, index_status, follow_up_question)
VALUES
-- OOP
('b2000000-0000-0000-0000-000000000001','11111111-1111-1111-1111-111111111111','vi','JUNIOR','[Fresher] Bốn trụ cột của lập trình hướng đối tượng là gì? Hãy giải thích ngắn bằng ví dụ Java.','Encapsulation ẩn trạng thái; Inheritance tái sử dụng hợp đồng kế thừa; Polymorphism chọn hành vi theo đối tượng runtime; Abstraction che chi tiết bằng interface/abstract class.','PUBLISHED','INDEXED','Nếu thay đổi một implementation, ví dụ nào có thể chứng minh Polymorphism đang giúp code client ổn định?'),
('b2000000-0000-0000-0000-000000000002','11111111-1111-1111-1111-111111111111','vi','MID','[Junior] Khi nào composition tốt hơn inheritance trong Java và vì sao?','Ưu tiên composition cho has-a hoặc reuse implementation; inheritance phải thực sự is-a và tuân thủ LSP; composition giảm coupling và tránh inheritance hierarchy sâu.','PUBLISHED','INDEXED','Bạn sẽ cấu trúc lại một class dùng inheritance để tái sử dụng utility như thế nào?'),
('b2000000-0000-0000-0000-000000000003','11111111-1111-1111-1111-111111111111','vi','SENIOR','[Middle] Một payment module đang vi phạm OCP vì mỗi loại thẻ thêm một nhánh if-else. Bạn sẽ thiết kế lại class và test như thế nào?','Tách abstraction PaymentGateway, triển khai theo strategy, dùng constructor injection, đóng gói domain result và mapping lỗi; kiểm tra strategy thay thế qua interface mock/contract test.','PUBLISHED','INDEXED','Bạn sẽ xử lý cấu hình gateway và fallback timeout ở mức service như thế nào?'),

-- Core Java
('b2000000-0000-0000-0000-000000000004','22222222-2222-2222-2222-222222222222','vi','JUNIOR','[Fresher] Checked exception và unchecked exception khác nhau thế nào trong Java?','Checked exception bắt buộc khai báo/xử lý, thường đại diện lỗi có thể khôi phục; unchecked là runtime exception cho lỗi lập trình hoặc điều kiện bất biến.','PUBLISHED','INDEXED','Bạn có nên wrap mọi SQLException thành RuntimeException không; vì sao?'),
('b2000000-0000-0000-0000-000000000005','22222222-2222-2222-2222-222222222222','vi','MID','[Junior] Vì sao nên dùng `equals` thay vì `==` để so sánh String và cách tránh String Pool phụ thuộc implementation?','String Pool chỉ tối ưu storage cho literal/intern; == so sánh reference, equals so sánh nội dung; không dựa vào intern để kiểm tra identity.','PUBLISHED','INDEXED','Trong hệ thống cache nhiều String, bạn sẽ tối ưu memory bằng cách nào?'),
('b2000000-0000-0000-0000-000000000006','22222222-2222-2222-2222-222222222222','vi','SENIOR','[Middle] API domain trả về một danh sách mutable chứa ID nội bộ. Bạn sẽ thiết kế model/DTO để giữ immutability và defensive copy?','Dùng immutable record/value object, không expose collection internal, sao chép khi nhận/trả mutable, validate invariant trong constructor và freeze/stream từ persistence.','PUBLISHED','INDEXED','Bạn đánh đổi giữa DTO copy và lazy projection trong endpoint có tải cao như thế nào?'),

-- Collections
('b2000000-0000-0000-0000-000000000007','33333333-3333-3333-3333-333333333333','vi','JUNIOR','[Fresher] Khi nào nên dùng ArrayList, LinkedList, HashSet và HashMap?','ArrayList truy cập index tốt; LinkedList thêm/xóa đầu tốt trong trường hợp nhất định; HashSet loại trùng; HashMap ánh xạ key-value và tra cứu trung bình O(1).','PUBLISHED','INDEXED','Bạn sẽ chọn cấu trúc nào để duy trì thứ tự chèn và truy cập nhanh?'),
('b2000000-0000-0000-0000-000000000008','33333333-3333-3333-3333-333333333333','vi','MID','[Junior] Một cache thống kê đang ghi concurrently từ nhiều thread. Vì sao HashMap không an toàn và lựa chọn thay thế là gì?','HashMap không thread-safe, có thể mất ghi hoặc corruption khi concurrent write; ConcurrentHashMap hỗ trợ compound operations một phần; cần quy ước immutable key/value và merge/compute atomically.','PUBLISHED','INDEXED','Nếu cần snapshot thống kê nhất quán, bạn sẽ đồng bộ thế nào?'),
('b2000000-0000-0000-0000-000000000009','33333333-3333-3333-3333-333333333333','vi','SENIOR','[Middle] Một Map chậm ở tải cao dù độ phức tạp lý thuyết là O(1). Bạn sẽ điều tra memory, collision và key equality như thế nào?','Thu thập profiler/GC, kiểm tra hashCode phân bố, mutable key, load factor, kích thước capacity và read/write pattern; benchmark JMH và đo allocation/cpu.','PUBLISHED','INDEXED','Bạn sẽ cân nhắc custom cache/primitive collection khi nào?'),

-- Java 8+
('b2000000-0000-0000-0000-00000000000a','44444444-4444-4444-4444-444444444444','vi','JUNIOR','[Fresher] Lambda expression và functional interface có quan hệ gì?','Lambda là implementation thân hàm cho functional interface có một abstract method; Java 8 còn cung cấp method reference và interface chuẩn như Function/Predicate/Consumer.','PUBLISHED','INDEXED','Khi nào method reference dễ đọc hơn lambda?'),
('b2000000-0000-0000-0000-00000000000b','44444444-4444-4444-4444-444444444444','vi','MID','[Junior] Một API trả Optional nhưng implementation dùng Optional cho entity field. Bạn sẽ sửa và vì sao?','Optional dành cho giá trị trả về có thể thiếu, không dành field/parameter/serialization; sửa getter/service boundary và cung cấp empty/exception ở use case rõ ràng.','PUBLISHED','INDEXED','Bạn dùng orElse hay orElseGet trong trường hợp tính giá trị mặc định tốn kém?'),
('b2000000-0000-0000-0000-00000000000c','44444444-4444-4444-4444-444444444444','vi','SENIOR','[Middle] Ghép ba API I/O bất đồng bộ mà không làm chặn common pool. Thiết kế CompletableFuture và timeout như thế nào?','Dùng supplyAsync với bounded executor riêng cho I/O, thenCombine/thenCompose đúng dependency graph, timeout bằng orTimeout, xử lý exception bằng exceptionally và join/complete boundary có kiểm soát.','PUBLISHED','INDEXED','Bạn xử lý partial failure và retry idempotency ở từng API như thế nào?'),

-- Concurrency
('b2000000-0000-0000-0000-00000000000d','55555555-5555-5555-5555-555555555555','vi','JUNIOR','[Fresher] `synchronized` và `volatile` khác nhau thế nào?','synchronized bảo đảm mutual exclusion và visibility; volatile bảo đảm visibility/ordering cho biến đơn lẻ nhưng không thay thế atomic compound operation.','PUBLISHED','INDEXED','Vì sao volatile không thể thay synchronized cho count++?'),
('b2000000-0000-0000-0000-00000000000e','55555555-5555-5555-5555-555555555555','vi','MID','[Junior] Hãy thiết kế một ThreadPoolExecutor cho service xử lý report và tránh queue quá tải.','Chọn core/max pool, bounded queue, rejection policy, thread factory, keep-alive và monitoring; lấy queue depth/active threads, backpressure và timeout thay vì chấp nhận mọi request.','PUBLISHED','INDEXED','Bạn sẽ dùng CallerRunsPolicy hay AbortPolicy trong tình huống HTTP traffic?'),
('b2000000-0000-0000-0000-00000000000f','66666666-6666-6666-6666-666666666666','vi','SENIOR','[Middle] Hai tài nguyên liên tiếp bị lock theo thứ tự khác nhau và xuất hiện deadlock trong production. Bạn khắc phục và xác minh ra sao?','Chuẩn hóa thứ tự lock, giảm critical section, dùng tryLock timeout, tái cấu trúc transaction, thu thập thread dump/JFR và tạo concurrency test hoặc stress test lặp lại.','PUBLISHED','INDEXED','Bạn sẽ dùng database row lock hay application lock theo nghiệp vụ nào?'),

-- JVM
('b2000000-0000-0000-0000-000000000010','66666666-6666-6666-6666-666666666666','vi','JUNIOR','[Fresher] Heap và Stack trong JVM khác nhau thế nào?','Stack chứa frame/thread và local reference; Heap lưu object/array dùng GC; mỗi thread có stack riêng, Heap dùng chung vùng GC.','PUBLISHED','INDEXED','StackOverflowError và OutOfMemoryError khác nhau thế nào?'),
('b2000000-0000-0000-0000-000000000011','66666666-6666-6666-6666-666666666666','vi','MID','[Junior] Garbage Collection hoạt động dựa trên cơ chế nào và G1GC ưu tiên điều gì?','GC đánh dấu reachability từ GC roots rồi thu gom unreachable object; G1 chia heap thành region và ưu tiên thu gom region có nhiều garbage trong pause target.','PUBLISHED','INDEXED','GC log nào cho thấy application đang chịu GC pressure?'),
('b2000000-0000-0000-0000-000000000012','66666666-6666-6666-6666-666666666666','vi','SENIOR','[Middle] Heap tăng liên tục sau mỗi deployment và load test. Hãy mô tả quy trình xác định memory leak.','Thu thập heap dump nhiều thời điểm, so sánh dominator/object count, truy vết GC root, kiểm tra cache/listener/thread/connection, fix và load test lâu hơn cùng GC/Xmx/container metrics.','PUBLISHED','INDEXED','Bạn xử lý trước khi restart container để tránh mất service như thế nào?'),

-- Spring Boot
('b2000000-0000-0000-0000-000000000013','77777777-7777-7777-7777-777777777777','vi','JUNIOR','[Fresher] Dependency Injection trong Spring là gì?','IoC container tạo và quản lý bean, resolve dependency bằng constructor/setter/field; constructor injection rõ ràng cho dependency bắt buộc.','PUBLISHED','INDEXED','Tại sao constructor injection dễ test và bảo vệ invariant hơn field injection?'),
('b2000000-0000-0000-0000-000000000014','77777777-7777-7777-7777-777777777777','vi','MID','[Junior] Một REST API cần xử lý validation và error response nhất quán. Bạn thiết kế thế nào?','Dùng Bean Validation ở request DTO, ControllerAdvice/ErrorResponse có code/status/message/details, HTTP status đúng, không lộ stack trace và giữ correlation ID.','PUBLISHED','INDEXED','Bạn phân biệt lỗi 400, 404, 409 và 422 như thế nào?'),
('b2000000-0000-0000-0000-000000000015','77777777-7777-7777-7777-777777777777','vi','SENIOR','[Middle] Một bean gọi @Transactional trong chính nó nhưng transaction không bật. Nguyên nhân và cách giải quyết là gì?','Spring proxy interceptor chỉ áp dụng khi gọi bean qua proxy; self-invocation bỏ qua proxy; tách bean service/transactional façade hoặc dùng cấu trúc gọi qua injected collaborator; test integration.','PUBLISHED','INDEXED','Bạn sẽ tổ chức transaction boundary trong Clean/Hexagonal Architecture thế nào?'),

-- Database JPA
('b2000000-0000-0000-0000-000000000016','88888888-8888-8888-8888-888888888888','vi','JUNIOR','[Fresher] ACID là gì trong cơ sở dữ liệu quan hệ?','Atomicity, Consistency, Isolation, Durability mô tả transaction phải rollback toàn phần, giữ ràng buộc, kiểm soát concurrent read/write và bền vững sau commit.','PUBLISHED','INDEXED','Ví dụ thực tế cho Isolation level READ_COMMITTED là gì?'),
('b2000000-0000-0000-0000-000000000017','88888888-8888-8888-8888-888888888888','vi','MID','[Junior] Hãy phát hiện và sửa lỗi N+1 trong JPA bằng SQL log thực tế.','Bật SQL logging/test SQL count, dùng join fetch/entity graph hoặc projection DTO, tránh EAGER không cần, kiểm tra transaction boundary và pagination riêng.','PUBLISHED','INDEXED','Fetch join có thể làm sai pagination hoặc nhân bản entity không?'),
('b2000000-0000-0000-0000-000000000018','88888888-8888-8888-8888-888888888888','vi','SENIOR','[Middle] Giao dịch chuyển tiền bị deadlock DB và timeout. Bạn sẽ thiết kế đồng bộ, retry và kiểm tra isolation như thế nào?','Chuyển sang optimistic/pessimistic lock theo domain, lock account theo thứ tự chuẩn, transaction ngắn, retry deadlock có backoff/idempotency và isolation phù hợp; kiểm tra lost update.','PUBLISHED','INDEXED','Bạn chọn pessimistic lock hay outbox/saga trong kiến trúc nhiều service?'),

-- Linux Networking
('b2000000-0000-0000-0000-000000000019','a1000000-0000-0000-0000-000000000001','vi','JUNIOR','[Fresher] Câu lệnh Linux nào giúp kiểm tra process, port và DNS cơ bản?','ps systemctl/ss kiểm tra process/socket; curl kiểm tra HTTP; getent/dig kiểm tra DNS; ip route kiểm tra routing.','PUBLISHED','INDEXED','Nếu curl localhost thành công nhưng từ máy khách thất bại, kiểm tra gì tiếp?'),
('b2000000-0000-0000-0000-00000000001a','a1000000-0000-0000-0000-000000000001','vi','MID','[Junior] Một reverse proxy trả 502 trong khi upstream vẫn chạy. Quy trình debug gồm những bước nào?','Kiểm tra DNS/upstream, socket, firewall, timeout và proxy log; xác nhận app bind address/port, headers và health endpoint; theo dõi access/error log.','PUBLISHED','INDEXED','Bạn sẽ đặt health check và timeout giữa proxy với upstream ra sao?'),
('b2000000-0000-0000-0000-00000000001b','a1000000-0000-0000-0000-000000000001','vi','SENIOR','[Middle] User truy cập HTTPS nhưng bị redirect loop khi traffic đi qua nhiều reverse proxy. Bạn sẽ phân tích header và TLS như thế nào?','Kiểm tra termination/TLS ở mỗi layer, forwarded proto/host/X-Forwarded-*; xác nhận app redirect scheme, proxy trust settings và đồng nhất Host/Origin; test từng hop.','PUBLISHED','INDEXED','Bạn sẽ tránh tin cặp header X-Forwarded-For từ client trực tiếp thế nào?'),

-- Docker
('b2000000-0000-0000-0000-00000000001c','a1000000-0000-0000-0000-000000000002','vi','JUNIOR','[Fresher] Dockerfile và Docker Image khác nhau thế nào?','Dockerfile là instruction build; Image là artifact read-only gồm các layers; container là runtime writable instance của image.','PUBLISHED','INDEXED','Một container thay đổi file bên trong thì image gốc có thay đổi không?'),
('b2000000-0000-0000-0000-00000000001d','a1000000-0000-0000-0000-000000000002','vi','MID','[Junior] Làm thế nào tối ưu image Spring Boot bằng multi-stage build?','Tách Maven/JDK build stage khỏi JRE runtime, cache dependencies trước source, copy artifact, chạy non-root, immutable tag, loại bỏ build tool và secret.','PUBLISHED','INDEXED','Bạn có nên copy toàn bộ local Maven cache vào runtime không?'),
('b2000000-0000-0000-0000-00000000001e','a1000000-0000-0000-0000-000000000002','vi','SENIOR','[Middle] Container bị OOM ở Kubernetes dù JVM heap lớn hơn memory limit. Bạn sẽ sửa Docker/JVM/resource như thế nào?','Đặt -Xms/-Xmx phù hợp container, theo dõi native/heap/metaspace/off-heap, đặt cgroup memory limit requests/limits, JVM UseContainerSupport và load test; không chỉ giảm heap.','PUBLISHED','INDEXED','Bạn kiểm tra native memory leak bằng công cụ nào ngoài heap dump?'),

-- Kubernetes
('b2000000-0000-0000-0000-00000000001f','a1000000-0000-0000-0000-000000000003','vi','JUNIOR','[Fresher] Pod, Deployment và Service trong Kubernetes lần lượt làm gì?','Pod chạy container; Deployment duy trì desired replica và rollout; Service cung cấp ổn định network/DNS/load balancing tới Pod.','PUBLISHED','INDEXED','Ingress khác Service như thế nào?'),
('b2000000-0000-0000-0000-000000000020','a1000000-0000-0000-0000-000000000003','vi','MID','[Junior] Pod ở trạng thái CrashLoopBackOff. Quy trình debug từng bước?','kubectl get/describe/logs previous, kiểm tra OOMKilled, exit code, config/secret, DB migration, probe và image; fix root cause rồi rollout/restart bounded.','PUBLISHED','INDEXED','Bạn phân biệt lỗi readiness và lỗi liveness probe thế nào?'),
('b2000000-0000-0000-0000-000000000021','a1000000-0000-0000-0000-000000000003','vi','SENIOR','[Middle] Deployment rollout làm latency tăng; bạn sẽ dùng các cơ chế rollout và readiness nào?','Dùng maxUnavailable/maxSurge, readiness probe, graceful shutdown/preStop, resource/PodDisruptionBudget, canary và rollback; theo dõi P95/error/saturation.','PUBLISHED','INDEXED','Bạn thiết kế zero-database-downtime migration song song code mới/cũ thế nào?'),

-- CI/CD
('b2000000-0000-0000-0000-000000000022','a1000000-0000-0000-0000-000000000004','vi','JUNIOR','[Fresher] CI và CD khác nhau ở điểm nào?','CI liên tục build/test/scan artifact; CD liên tục chuẩn bị và triển khai qua staging/production theo policy, có thể manual approval.','PUBLISHED','INDEXED','Deployment pipeline có cần đi kèm Automated Testing không?'),
('b2000000-0000-0000-0000-000000000023','a1000000-0000-0000-0000-000000000004','vi','MID','[Junior] Thiết kế GitLab CI cho Spring Boot với test, build image và deploy staging.','Dùng stage test/build/deploy, cache dependency hợp lý, immutable image tag, scan, testcontainers/integration test, smoke test và rollout status.','PUBLISHED','INDEXED','Bạn sẽ tránh chạy Docker privileged trong runner không tin cậy thế nào?'),
('b2000000-0000-0000-0000-000000000024','a1000000-0000-0000-0000-000000000004','vi','SENIOR','[Middle] Pipeline đã test thành công nhưng production gặp lỗi do artifact và config khác nhau. Giải pháp governance là gì?','Giữ one build artifact promoted across environments, digest/immutable tags, config externalization, release manifest, SBOM/signature, smoke test và rollback.','PUBLISHED','INDEXED','Bạn sẽ kiểm chứng artifact provenance và signature trước deploy thế nào?'),

-- Monitoring
('b2000000-0000-0000-0000-000000000025','a1000000-0000-0000-0000-000000000005','vi','JUNIOR','[Fresher] Metrics, logs và traces khác nhau thế nào?','Metrics đo số liệu theo thời gian, logs ghi event chi tiết, traces theo request xuyên microservices; cùng đóng góp observability.','PUBLISHED','INDEXED','Một request chậm cần kết hợp cả ba dữ liệu ra sao?'),
('b2000000-0000-0000-0000-000000000026','a1000000-0000-0000-0000-000000000005','vi','MID','[Junior] Thiết kế Prometheus/Grafana alert cho API latency và error rate.','Dùng RED/USE, histogram P95/P99, alert theo SLO và thời gian duy trì, giảm alert noise, dashboard liên kết trace/log và runbook.','PUBLISHED','INDEXED','High-cardinality label là nguy cơ gì trong Prometheus?'),
('b2000000-0000-0000-0000-000000000027','a1000000-0000-0000-0000-000000000005','vi','SENIOR','[Middle] Mức SLO 99.9% bị phá do microservice phụ thuộc. Bạn sẽ điều tra và xây observability theo distributed trace như thế nào?','Bấm trace sampling phù hợp, correlation ID, baggage/context, phân tích span duration/error/dependency, ví dụ tail sampling, dashboard theo SLO và alert theo burn rate.','PUBLISHED','INDEXED','Bạn chọn 100% sampling hay tail-based sampling trong workload cao?'),

-- Microservices
('b2000000-0000-0000-0000-000000000028','a1000000-0000-0000-0000-000000000006','vi','JUNIOR','[Fresher] REST, gRPC và Message Broker khác nhau thế nào?','REST HTTP resource thường cho public/integration, gRPC binary RPC cho internal high performance, broker event buffering và decoupling bất đồng bộ.','PUBLISHED','INDEXED','Khi nào RPC đồng bộ không phù hợp?'),
('b2000000-0000-0000-0000-000000000029','a1000000-0000-0000-0000-000000000006','vi','MID','[Junior] Consumer nhận message trùng. Hãy thiết kế idempotency và retry/DLQ.','Dùng business id/request key unique, transaction dedupe, retry bounded với backoff, DLQ sau khi vượt ngưỡng, correlation ID và monitor poison message.','PUBLISHED','INDEXED','Bạn lưu dedupe record bao lâu và cleanup thế nào?'),
('b2000000-0000-0000-0000-00000000002a','a1000000-0000-0000-0000-000000000006','vi','SENIOR','[Middle] Một hệ thống distributed không thể dùng transaction ACID xuyên service. Bạn sẽ thiết kế saga/outbox và nhất quán cuối cùng như thế nào?','Dùng transactional outbox publish reliable, correlation ID, saga compensation, idempotent consumer, state machine, monitor in-flight event và explain eventual consistency.','PUBLISHED','INDEXED','Bạn xử lý compensation fail và orphaned saga instance thế nào?')
ON CONFLICT (id) DO UPDATE SET
    topic_id=EXCLUDED.topic_id,
    language=EXCLUDED.language,
    level=EXCLUDED.level,
    content=EXCLUDED.content,
    reference_answer=EXCLUDED.reference_answer,
    status=EXCLUDED.status,
    index_status=EXCLUDED.index_status,
    follow_up_question=EXCLUDED.follow_up_question,
    updated_at=CURRENT_TIMESTAMP;

-- ================================================================
-- 5. Key points cho 42 câu hỏi
-- ================================================================
INSERT INTO question_key_points (question_id, seq, content)
SELECT question_id, seq, content
FROM (VALUES
('b2000000-0000-0000-0000-000000000001'::uuid,1,'Nhận diện đầy đủ Encapsulation, Inheritance, Polymorphism, Abstraction'),
('b2000000-0000-0000-0000-000000000001'::uuid,2,'Giải thích đúng vai trò của mỗi trụ cột bằng ví dụ Java'),
('b2000000-0000-0000-0000-000000000001'::uuid,3,'Không nhầm lẫn abstraction với implementation details'),
('b2000000-0000-0000-0000-000000000002'::uuid,1,'Phân biệt is-a với has-a'),
('b2000000-0000-0000-0000-000000000002'::uuid,2,'Ưu tiên composition để giảm coupling'),
('b2000000-0000-0000-0000-000000000002'::uuid,3,'Nhắc đến Liskov substitution và inheritance hierarchy sâu'),
('b2000000-0000-0000-0000-000000000003'::uuid,1,'Tách strategy hoặc payment abstraction'),
('b2000000-0000-0000-0000-000000000003'::uuid,2,'Áp dụng constructor injection và domain result'),
('b2000000-0000-0000-0000-000000000003'::uuid,3,'Test thay implementation và fallback timeout'),
('b2000000-0000-0000-0000-000000000004'::uuid,1,'Phân biệt compile-time checked exception'),
('b2000000-0000-0000-0000-000000000004'::uuid,2,'Giải thích RuntimeException và tình huống bất biến'),
('b2000000-0000-0000-0000-000000000004'::uuid,3,'Cách xử lý exception cần phục hồi được'),
('b2000000-0000-0000-0000-000000000005'::uuid,1,'equals so sánh nội dung, == so sánh reference'),
('b2000000-0000-0000-0000-000000000005'::uuid,2,'String literal có thể nằm trong String Pool'),
('b2000000-0000-0000-0000-000000000005'::uuid,3,'Không dựa vào intern hoặc == để kiểm tra đồng nhất'),
('b2000000-0000-0000-0000-000000000006'::uuid,1,'Giữ immutable state bằng final và defensive copy'),
('b2000000-0000-0000-0000-000000000006'::uuid,2,'Không expose collection hoặc mutable field'),
('b2000000-0000-0000-0000-000000000006'::uuid,3,'Cân nhắc copy DTO với persistence projection'),
('b2000000-0000-0000-0000-000000000007'::uuid,1,'Chọn ArrayList cho truy cập index'),
('b2000000-0000-0000-0000-000000000007'::uuid,2,'Chọn HashMap/HashSet theo key hoặc unique value'),
('b2000000-0000-0000-0000-000000000007'::uuid,3,'Biết LinkedList không tự động là lựa chọn tốt cho mọi trường hợp'),
('b2000000-0000-0000-0000-000000000008'::uuid,1,'HashMap không thread-safe'),
('b2000000-0000-0000-0000-000000000008'::uuid,2,'ConcurrentHashMap hỗ trợ concurrent read/write'),
('b2000000-0000-0000-0000-000000000008'::uuid,3,'Dùng atomic merge/compute và immutable key'),
('b2000000-0000-0000-0000-000000000009'::uuid,1,'Kiểm tra hashCode phân phối và mutable key'),
('b2000000-0000-0000-0000-000000000009'::uuid,2,'Kiểm tra collision, load factor và GC allocation'),
('b2000000-0000-0000-0000-000000000009'::uuid,3,'Benchmark bằng profiler/JMH trước khi tối ưu'),
('b2000000-0000-0000-0000-00000000000a'::uuid,1,'Lambda implement functional interface'),
('b2000000-0000-0000-0000-00000000000a'::uuid,2,'Nhận diện một abstract method'),
('b2000000-0000-0000-0000-00000000000a'::uuid,3,'Sử dụng method reference khi code rõ ràng hơn'),
('b2000000-0000-0000-0000-00000000000b'::uuid,1,'Optional chỉ dùng cho return value'),
('b2000000-0000-0000-0000-00000000000b'::uuid,2,'Không dùng Optional cho field/parameter'),
('b2000000-0000-0000-0000-00000000000b'::uuid,3,'orElseGet dùng khi fallback tính tốn kém'),
('b2000000-0000-0000-0000-00000000000c'::uuid,1,'Tách executor I/O bounded'),
('b2000000-0000-0000-0000-00000000000c'::uuid,2,'Dùng thenCombine/thenCompose theo dependency'),
('b2000000-0000-0000-0000-00000000000c'::uuid,3,'Có timeout, exception handling và join boundary'),
('b2000000-0000-0000-0000-00000000000d'::uuid,1,'synchronized bảo đảm mutual exclusion và visibility'),
('b2000000-0000-0000-0000-00000000000d'::uuid,2,'volatile bảo đảm visibility nhưng không atom compound'),
('b2000000-0000-0000-0000-00000000000d'::uuid,3,'count++ không thread-safe chỉ nhờ volatile'),
('b2000000-0000-0000-0000-00000000000e'::uuid,1,'Thiết lập bounded core/max pool'),
('b2000000-0000-0000-0000-00000000000e'::uuid,2,'Chọn queue, rejected policy và thread factory'),
('b2000000-0000-0000-0000-00000000000e'::uuid,3,'Theo dõi queue depth và backpressure'),
('b2000000-0000-0000-0000-00000000000f'::uuid,1,'Chuẩn hóa thứ tự lock'),
('b2000000-0000-0000-0000-00000000000f'::uuid,2,'Giảm critical section và dùng tryLock timeout'),
('b2000000-0000-0000-0000-00000000000f'::uuid,3,'Thu thập thread dump/JFR và viết stress test'),
('b2000000-0000-0000-0000-000000000010'::uuid,1,'Stack chứa frame và local reference'),
('b2000000-0000-0000-0000-000000000010'::uuid,2,'Heap chứa object/array và được GC quản lý'),
('b2000000-0000-0000-0000-000000000010'::uuid,3,'Phân biệt StackOverflow và OutOfMemory'),
('b2000000-0000-0000-0000-000000000011'::uuid,1,'GC xác định reachability từ GC roots'),
('b2000000-0000-0000-0000-000000000011'::uuid,2,'G1 chia heap thành region'),
('b2000000-0000-0000-0000-000000000011'::uuid,3,'G1 tối ưu pause và thu gom region nhiều garbage'),
('b2000000-0000-0000-0000-000000000012'::uuid,1,'Thu thập heap dump nhiều thời điểm'),
('b2000000-0000-0000-0000-000000000012'::uuid,2,'So sánh dominator/object count và GC root'),
('b2000000-0000-0000-0000-000000000012'::uuid,3,'Kiểm tra cache/listener/thread/connection và load test'),
('b2000000-0000-0000-0000-000000000013'::uuid,1,'IoC container tạo và resolve bean'),
('b2000000-0000-0000-0000-000000000013'::uuid,2,'Constructor injection cho dependency bắt buộc'),
('b2000000-0000-0000-0000-000000000013'::uuid,3,'Dependency Injection làm class dễ test'),
('b2000000-0000-0000-0000-000000000014'::uuid,1,'Bean Validation trên request DTO'),
('b2000000-0000-0000-0000-000000000014'::uuid,2,'ControllerAdvice trả error contract nhất quán'),
('b2000000-0000-0000-0000-000000000014'::uuid,3,'Không lộ stack trace và dùng correlation ID'),
('b2000000-0000-0000-0000-000000000015'::uuid,1,'Spring proxy tạo transactional interceptor'),
('b2000000-0000-0000-0000-000000000015'::uuid,2,'Self-invocation bỏ qua proxy'),
('b2000000-0000-0000-0000-000000000015'::uuid,3,'Tách transactional collaborator và test integration'),
('b2000000-0000-0000-0000-000000000016'::uuid,1,'Giải thích Atomicity và Consistency'),
('b2000000-0000-0000-0000-000000000016'::uuid,2,'Giải thích Isolation và concurrent access'),
('b2000000-0000-0000-0000-000000000016'::uuid,3,'Giải thích Durability sau commit'),
('b2000000-0000-0000-0000-000000000017'::uuid,1,'Phát hiện bằng SQL count/log'),
('b2000000-0000-0000-0000-000000000017'::uuid,2,'Dùng join fetch/entity graph/DTO projection'),
('b2000000-0000-0000-0000-000000000017'::uuid,3,'Kiểm tra fetch plan và pagination'),
('b2000000-0000-0000-0000-000000000018'::uuid,1,'Dùng pessimistic/optimistic lock phù hợp'),
('b2000000-0000-0000-0000-000000000018'::uuid,2,'Giữ transaction ngắn và lock theo thứ tự'),
('b2000000-0000-0000-0000-000000000018'::uuid,3,'Retry deadlock có backoff và idempotency'),
('b2000000-0000-0000-0000-000000000019'::uuid,1,'ps/systemctl kiểm tra process'),
('b2000000-0000-0000-0000-000000000019'::uuid,2,'ss/curl kiểm tra port và HTTP'),
('b2000000-0000-0000-0000-000000000019'::uuid,3,'getent/dig kiểm tra DNS'),
('b2000000-0000-0000-0000-00000000001a'::uuid,1,'Kiểm tra upstream DNS/socket/firewall'),
('b2000000-0000-0000-0000-00000000001a'::uuid,2,'Đọc proxy và application log'),
('b2000000-0000-0000-0000-00000000001a'::uuid,3,'Kiểm tra timeout, bind address và health'),
('b2000000-0000-0000-0000-00000000001b'::uuid,1,'Kiểm tra TLS termination từng hop'),
('b2000000-0000-0000-0000-00000000001b'::uuid,2,'Đồng nhất forwarded proto/host'),
('b2000000-0000-0000-0000-00000000001b'::uuid,3,'Cấu hình trusted proxy thay vì tin header mọi nguồn'),
('b2000000-0000-0000-0000-00000000001c'::uuid,1,'Dockerfile là instruction build'),
('b2000000-0000-0000-0000-00000000001c'::uuid,2,'Image là tập read-only layers'),
('b2000000-0000-0000-0000-00000000001c'::uuid,3,'Container có writable layer runtime'),
('b2000000-0000-0000-0000-00000000001d'::uuid,1,'Tách build và runtime stage'),
('b2000000-0000-0000-0000-00000000001d'::uuid,2,'Cache Maven dependency trước source'),
('b2000000-0000-0000-0000-00000000001d'::uuid,3,'Chạy non-root và dùng immutable artifact'),
('b2000000-0000-0000-0000-00000000001e'::uuid,1,'Xms/Xmx phù hợp container memory'),
('b2000000-0000-0000-0000-00000000001e'::uuid,2,'Theo dõi native/heap/metaspace/off-heap'),
('b2000000-0000-0000-0000-00000000001e'::uuid,3,'Load test với cgroup limits'),
('b2000000-0000-0000-0000-00000000001f'::uuid,1,'Pod chạy container'),
('b2000000-0000-0000-0000-00000000001f'::uuid,2,'Deployment quản lý replica và rollout'),
('b2000000-0000-0000-0000-00000000001f'::uuid,3,'Service cung cấp DNS/load balancing'),
('b2000000-0000-0000-0000-000000000020'::uuid,1,'get/describe/log previous'),
('b2000000-0000-0000-0000-000000000020'::uuid,2,'Kiểm tra OOMKilled và exit code'),
('b2000000-0000-0000-0000-000000000020'::uuid,3,'Kiểm tra config/secret/probe/image'),
('b2000000-0000-0000-0000-000000000021'::uuid,1,'Thiết lập maxSurge/maxUnavailable'),
('b2000000-0000-0000-0000-000000000021'::uuid,2,'Cấu hình readiness và graceful shutdown'),
('b2000000-0000-0000-0000-000000000021'::uuid,3,'Canary/rollback theo P95 và error rate'),
('b2000000-0000-0000-0000-000000000022'::uuid,1,'CI build/test/scan liên tục'),
('b2000000-0000-0000-0000-000000000022'::uuid,2,'CD deploy qua environment policy'),
('b2000000-0000-0000-0000-000000000022'::uuid,3,'Có approval hoặc automated validation'),
('b2000000-0000-0000-0000-000000000023'::uuid,1,'Stages test/build/deploy'),
('b2000000-0000-0000-0000-000000000023'::uuid,2,'Immutable image tag và cache Maven'),
('b2000000-0000-0000-0000-000000000023'::uuid,3,'Smoke test và rollout status'),
('b2000000-0000-0000-0000-000000000024'::uuid,1,'Promote một build artifact'),
('b2000000-0000-0000-0000-000000000024'::uuid,2,'Externalize config và release manifest'),
('b2000000-0000-0000-0000-000000000024'::uuid,3,'Xác thực SBOM/signature và rollback'),
('b2000000-0000-0000-0000-000000000025'::uuid,1,'Metrics đo số liệu theo thời gian'),
('b2000000-0000-0000-0000-000000000025'::uuid,2,'Logs ghi event chi tiết'),
('b2000000-0000-0000-0000-000000000025'::uuid,3,'Traces theo request xuyên service'),
('b2000000-0000-0000-0000-000000000026'::uuid,1,'Đo P95/P99 latency và error rate'),
('b2000000-0000-0000-0000-000000000026'::uuid,2,'Alert theo SLO và for duration'),
('b2000000-0000-0000-0000-000000000026'::uuid,3,'Tránh high-cardinality metric label'),
('b2000000-0000-0000-0000-000000000027'::uuid,1,'Dùng correlation ID và trace context'),
('b2000000-0000-0000-0000-000000000027'::uuid,2,'Phân tích span latency/error/dependency'),
('b2000000-0000-0000-0000-000000000027'::uuid,3,'Dùng burn-rate alert và sampling phù hợp'),
('b2000000-0000-0000-0000-000000000028'::uuid,1,'REST cho resource/public integration'),
('b2000000-0000-0000-0000-000000000028'::uuid,2,'gRPC cho internal low-latency RPC'),
('b2000000-0000-0000-0000-000000000028'::uuid,3,'Broker cho event-driven decoupling'),
('b2000000-0000-0000-0000-000000000029'::uuid,1,'Dedupe bằng business ID/unique constraint'),
('b2000000-0000-0000-0000-000000000029'::uuid,2,'Retry bounded với backoff'),
('b2000000-0000-0000-0000-000000000029'::uuid,3,'DLQ và correlation ID'),
('b2000000-0000-0000-0000-00000000002a'::uuid,1,'Dùng transactional outbox'),
('b2000000-0000-0000-0000-00000000002a'::uuid,2,'Dùng saga compensation và state machine'),
('b2000000-0000-0000-0000-00000000002a'::uuid,3,'Idempotent consumer và monitor orphan saga')
) AS k(question_id, seq, content)
ON CONFLICT DO NOTHING;
