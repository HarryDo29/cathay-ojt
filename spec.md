Cathay OJT

API gateway
- Là entry duy nhất mà để nhận request từ clients và chuyển đến các microservices, giúp che đi các service con bên trong
- Thực hiện các vấn đề cross-cutting: authentication, rate limit, load balancing và logging
    - Cross-cutting là chức năng phụ (khía cạnh) liên quan đến nhiều ứng dụng (cắt ngang qua các business logic —> gây lặp code nhiều lần)
- Nhiệm vụ:
    - Routing: chuyển các request đến đúng service
    - Security: thực hiện authentication và authorization, chống tấn công
    - Rate limit: ngăn chặn hệ thống qua tải bằng cách giới hạn req trong 1 khoảng thời gian
    - Monitor & Logging: giám sát hệ thông
    - Caching: lưu trữ data tạm thời —> tăng hiệu suất
    - Transformation: biến đổi kiểu dữ liệu (định dạng req/res)
    - Aggregation: kết hợp nhiều lệnh gọi service thành 1 yêu cầu duy nhất
 Cross-cutting concerns:
	- là 1 phần của chương trình phụ thuộc hoặc phải ảnh hưởng bởi nhiều module hoàn chỉnh khác. 
	- ứng dụng thường được chia làm các module, tuy nhiên trong những module này tồn tại những tác vụ bắt buộc —> tạo ra sự lặp lại —> cross-cutting

	- Phổ biến: Logging, Security, Transaction Manager, Caching, Error handling.

Trong API Gateway

CORS: sinh ra vì same-origin policy,  là 1 chính sách liên quan đến bảo mật và được cài vào các trình duyệt hiện nay —> ngăn chặn việc truy cập vô tội vạ vào các domain khác. 
	Là một cơ chế bảo mật của Trình duyệt (Browser), không phải của Server.
- COR sinh ra: để nói lỏng Same-origin policy
    - Origin: protocol + domain + port
    - CORS hoạt động: trước khi gửi 1 request thật thì sẽ có 1 pre-light request  vs method OPTIONS để browser kt xem ở domain khác có cho phép domain này không? Nếu chấp nhận thì sẽ gửi lại bằng 1 response vs header bao gồm Access-Control-Allow-Origin, Access-Control-Allow-Methods, Access-Control-Allow-Headers, Access-Control-Max-Age để xác nhận. Sau đó thì request thật sự mới đc gửi dì.
    - Tại sao lại sinh ra CORS: ngăn chặn tấn công CSRF (Cross-site Request Forgery) 
    - Lưu ý allowCredentials và allowOrigins: nếu allowCredentials là true —> allowOrigins ko được là *. Vì tất cả các origin sẽ đều truy cập được vào cookies/session
    - Nếu đã có sét up CORS ở api gateway thì không cần ở các service con khác

Authentication (xác thực): là 1 điểm xác minh danh tính người dùng tại 1 thời điểm duy nhất trước khi chuyển đến các service con —> bảo mật tập trung và giảm tải cho các service con.
	- là nơi áp dụng các cơ chế JWT, OAuth 2 để kiểm tra
	- JWT: phổ biến nhất, kiểm tra JWT đảm bảo tính hợp lệ mà không cần truy vấn trong database
	- OAuth 2:
	
	Lợi ích
	- Tập trung hóa bảo mật: chỉ cần xác thực 1 lần trước khi đến các micro service khác, nếu không hợp lệ thì sẽ không tới được các micro service
	- Hỗ trợ nhiều phương thức
	- Quản lí phiên

	Quy trình với JWT:
	- Client gửi req kèm header Authorization: `Bearer [JWT]`
	- API Gateway nhận req và kiểm tra (nếu không JWT: giải mã, hạn sử dụng và chữ kí có hợp lệ hay không)
		- Nếu hợp lệ: API Gateway sẽ lấy user in4 theo dạng key:value và gắn vào header —> đến các filter tiếp theo trước khi đến các micro service
		- Nếu không hợp lệ: API Gateway sẽ bắn về cho client 1 lỗi 401 Unauthorized

Authorization (phân quyền): 	 là quá trình xác thực người dùng sau khi authentication có quyền thực hiện những hành động cụ thể nào 
	- Phân quyền tập trung: kiểm tra quyền hạn của user trước khi forward req
		Ưu điểm: giảm tải logic cho các micro service
		Nhược điểm: các thể bị tắc nghẽn nếu logic quá phức tạp
	- Chiến lược phân quyền: 
		- RBAC (Role-Based Access Control): kiểm tra dựa trên role
		- ABAC (Attribute-Based Access Control): phân quyền dựa trên thuộc tính của user, tài nguyên và môi trường
	- Hoạt động:
		- Trích xuất token từ Header
		- Giải mã và Kiểm tra JWT —> hạn sử dụng và chữ kí
		- Kiểm tra claim: dựa vào role hoặc scope
		- Đối chiếu với endpoints của req nếu phù hợp thì chuyển tiếp req, nếu không khớp thì trả về 403 Forbidden
	- Mô hình phân quyền 2 lớp
		- Ở API gateway: kiểm tra tổng quát, user có quyền truy cập vào cụm api này hay ko —> Không thì chặn ở API Gateway và trả về 403 Forbidden
		- Ở các microservice: kiểm tra chi tiết hơn xem user có thực sự cs quyền truy cập hay không	 

SSL (Secure Socket Layer)
	- là giao thức bảo mật đời đầu, phát triển bởi Netscape vào những năm 1990
	- Nhiệm vụ: mã hóa dữ liệu trên internet để bảo vệ thông tin người dùng (tránh bị chỉnh sửa các thông tin bảo mật)
	- Trạng thái hiện tại: đã lỗi thời —> do có nhiều lỗ hổng bảo mật

TLS (Transport Layer Security)
	- là phiên bản nâng cấp hiện đại và bảo mật hơn của SSL
	- Nhiệm vụ: hoàn toàn giống SSL nhưng được sử dụng phương thức mã hóa mạnh mẽ hơn và quy trình handshake phức tạp hơn
	- Trạng thái hiện tại: tiêu chuẩn chung (hầu hết trang web đều sẽ cài chứng chỉ bảo mật này)

Mặc dù: SSL vẫn được sử dụng phổ biến nhưng thực chất về mặt kĩ thuật là sử dụng TLS bản 1.2 or 1.3

	SSL/TLS sử dụng ntn:
		- Client Hello: Browser duyệt phiên bản TLS hỗ trợ và danh sách các thuật toán mã hóa
		- Server Hello & Certificate: Server gửi lại certificate (SSL/TLS certificate) chứa public key
		- Authentication: Browser sẽ kiểm tra xem chứng chỉ có hợp lệ không 
		- Key Exchange: Browser sẽ tạo ra 1 Session Key bí mật, dùng public key của server để mã hóa và gửi đi
		- Decryption: Server dụng private key để giải mã lấy Session Key
		- Secure Connection: từ đây server và browser sử dụng Session Key để mã hóa dữ liệu theo kiểu đối xứng.
	 SSL/TLS trong kiến trúc API Gateway
		- SSL Terminal: Gateway nhận request từ HTTPS và giải mã rồi gửi HTTP req (không mã hóa) đến các microservice.
			Ưu điểm: giảm tải cv cho các micro service, quản lí chứng chỉ tập trung tại API Gateway. 
		- SSL Passthrough: Gateway chỉ chuyển tiếp HTTPS req đến các micro service (tự giải mã)
			Ưu điểm: Bảo mật tối đa trong nội bộ (tăng cv lên các server backend nội bộ) 
	
	Tại sao cần SSL/TLS:
		- Mã hóa dữ liệu: tránh bị tấn công Man-In-The-Middle
		- Xác thực: đảm bảo server bạn kết nối là server chính chủ
		- Toàn vẹn dữ liệu: dữ liệu toàn vẹn —> đảm bảo ko bị thay đổi
		- SEO: Google tin tưởng tuyệt đối các trang web https

Monitor:  là giám sát của toàn bộ hệ thống, giám sát hiệu suất và hoạt động của các API —> giúp theo dõi các chỉ số quan trọng (end point, độ trễ, lỗi, ….) quản lí lưu lượng, giảm sát trạng thái các các microservice
	- Metrics (chỉ số định lượng): 
		- Throughput: số lượng request mỗi giây (RPS)
		- Latency: thời gian phản hồi. 
			Thời giản phản hồi của api gateway
			Thời gian chờ phản hồi của các microservice
		- Error Rate: tỷ lệ lỗi 4xx (lỗi khách hàng) và 5xx (lỗi hệ thống)
		- Resource Usage: CPU và RAM của hệ thống đang chạy

	- Logging (nhật kí chi tiết): 
		- Access Logs: log từng thông tin req 
				Nội dung: {
					Timestamp, 
					Client IP, 
					HTTP Method, 
					Request Path, 
					Status Code, 
					Latency (thời gian xử lý),
					User-Agent
				}
		- Error Logs: log error để tìm ra nguyên nhân của api gateway. 
				Nội dung: {
					Stack trace, 
					thông báo lỗi từ Gateway (ví dụ: không tìm thấy service, timeout, hoặc lỗi lọc tham số).
				}
		- Logging tập trung: thay vì phải xem log ở api gateway và từng microservice thì logging tập trung sinh ra để thay thế
			Lợi ích: 
				- Chỉ cần mở 1 giao diện để xem logging của toàn hệ thống.
				- Xử lí sự cố ngay lập tức: xác minh lõi từ service nào
				- Phân tích dữ liệu: dựa vào logging để phân tích

	- Distributed tracing (truy vết phân tán): gateway sinh ra 1 Correlation ID (hoặc Trace ID) duy nhất cho mỗi req, và đính kèm vào header. 
		Mỗi req có thể sẽ đi qua nhiều micro service khác nhau
			—> Mục định: khi xảy ra lỗi thì ta có thể dựa vào id đó để thấy toàn bộ hành trình của api đó
		
		2 thực thể: 
			- Trace (hành trình): life cycle của 1 request
			- Span (phân đoạn): đại diện cho 1 phần trong các công đoạn của req
		
		Cơ chế hoạt động:
			- Tại api gateway, sẽ sinh ra 1 trace id và trace id sẽ được gắn vào header của req vs key X-B3-TraceId
			- Khi đến các service con, thì chính các service con sẽ lấy trace id để log nội bộ
			- Trace id sẽ tồn tại đến khi request còn kết thúc (dead)

	- Health Checks: Gateway sẽ kiểm tra các service con liên tục nếu service lỗi thì api gateway sẽ không chuyển tiếp request đến service đó và kết hợp hợp vs Circuit Breaker
		Có 2 cơ chế để kiểm tra:
		- Active Health Check: Gateway sẽ gửi pre-light req “thăm dò” đến 1 endpoint “/health” định kì (sd 5s 1 lần). 
				Ưu điểm: phát hiện lỗi không cần có traffic từ user.
		- Passive Health Check: gateway sẽ quan sát lượng req, nếu như có 5 req liên tiếp đến service A đều trả về lỗi 5xx —> chứng minh service đó bị lỗi —> là cơ chế Circuit Break