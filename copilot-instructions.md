# GitHub Copilot Instructions for Cuppacino-Server Project

## Project Context

This is a WebSocket-based microservices reflection library that:
- Uses WebSockets for all communication (NOT HTTP)
- Forces async-only design (all methods return `CompletableFuture<T>`)
- Supports multiple serialization formats (Custom Binary, Protocol Buffers, JSON)
- Implements reflection-based proxies for clean interface abstractions

## Code Style Rules

### Always Follow These Rules:

1. **No Summary Files**
   - Do NOT create summary/completion markdown files after each operation
   - Provide a concise summary in chat instead
   - Design documentation and technical specifications ARE acceptable and encouraged
   - Only create documentation files when explicitly requested or for design/architecture
   - Focus on code changes, not meta-documentation about what was just done

2. **Code Reuse and Refactoring**
   - Always check for pre-existing patterns before creating new code
   - Extract common functionality into utility classes
   - Refactor duplicated logic into shared methods
   - Avoid singletons - prefer dependency injection or factory patterns
   - Look for opportunities to consolidate similar implementations

3. **Testing and Verification**
   - Always create unit tests for new functionality
   - Verify code compiles and tests pass before claiming completion
   - Use `get_errors` tool to check for compilation errors
   - Run tests with appropriate frameworks (JUnit, Mockito, etc.)
   - Never claim work is complete without verification

4. **Confidence Levels and Verification Requests**
   - Always provide a confidence level when completing work (e.g., "95% confident", "Medium confidence")
   - Explicitly list areas that should be checked or verified
   - Highlight assumptions made during implementation
   - Suggest specific tests or scenarios to validate the solution
   - Be transparent about limitations or edge cases not handled

5. **Dependency Injection (New Code Only)**
   - Use lwdi API for dependency injection in NEW functionality only
   - Do NOT refactor existing code to use DI unless explicitly requested
   - Prefer constructor injection over singletons
   - Use `@DI` annotation on constructors that should be auto-injected

6. **Only Perform Explicitly Requested Operations**
   - Only perform operations that are explicitly requested by the user
   - Do NOT make unrequested changes or refactorings
   - Do NOT clean up or modify existing code unless specifically asked
   - Ask for clarification if the request is ambiguous

7. **Self-Describing Code and Minimal Comments**
   - Limit comments to code that is not self-descriptive
   - Work to keep code self-describing instead of using comments
   - Use clear, descriptive variable and method names
   - Only add comments when the "why" isn't obvious from the code itself
   - Remove unnecessary comments that just repeat what the code does

8. **WebSocket-Only Design**
   - Never suggest HTTP endpoints, REST paths, or HTTP methods
   - Use WebSocket operations identified by name
   - All communication goes through `WebSocketRequest` and `WebSocketResponse`

9. **Async-Only APIs**
   - ALL service methods MUST return `CompletableFuture<T>`
   - Never create synchronous blocking methods
   - Use `.thenApply()`, `.thenCompose()`, `.exceptionally()` for composition

10. **No HTTP Annotations**
   - Don't use `@Body`, `@Header`, `@PathParam`, `@QueryParam`, or `HttpMethod`
   - Use plain method parameters (they're serialized automatically)
   - Valid annotations: `@RemoteService`, `@RemoteMethod`, `SerializationFormat`

11. **Clean Interface Design**
   - Use descriptive operation names
   - Return types should be wrapped in `CompletableFuture<T>`
   - Parameters are automatically serialized based on the service's format

12. **Performance Focus**
   - Prefer Custom Binary serialization for internal services
   - Use Protocol Buffers for cross-platform compatibility
   - Use JSON only for debugging or external APIs
   - Always consider ThreadLocal buffer reuse for hot paths

## Project Structure

### Main Packages:
- `com.kalynx.commsapi.annotations` - WebSocket annotations (@RemoteService, @RemoteMethod)
- `com.kalynx.commsapi.protocol` - WebSocketRequest/WebSocketResponse structures
- `com.kalynx.commsapi.examples` - Example service interfaces
- `com.kalynx.commsapi.performancebenchmark` - Serialization benchmarks (test scope)

### Key Files:
- `WEBSOCKET_DESIGN.md` - Primary design document (USE THIS as reference)
- `HTTP_CLEANUP_SUCCESS.md` - What was removed and why
- `WEBSOCKET_PIVOT_SUMMARY.md` - Transition from HTTP to WebSocket

## Testing Guidelines

1. All serialization tests go in `performancebenchmark` package (test scope)
2. Tests should validate correctness AND performance
3. Use 100K-1M iterations for meaningful performance measurements
4. Always include both serialization and deserialization benchmarks

## Common Patterns

### Creating Service Interfaces:
```java
@RemoteService(
    serviceName = "service-name",
    endpoint = "ws://${host}:${port}/ws",
    format = SerializationFormat.CUSTOM_BINARY,
    timeout = 5000,
    autoReconnect = true
)
public interface MyService {
    @RemoteMethod(operation = "operationName", timeout = 2000)
    CompletableFuture<Result> doSomething(String param);
}
```

### Using Services:
```java
MyService service = ServiceProxyFactory.create(MyService.class);

service.doSomething("value")
    .thenApply(result -> /* transform */)
    .thenCompose(next -> /* chain another async call */)
    .exceptionally(ex -> /* handle errors */);
```

### Mocking for Tests:
```java
MyService mock = Mockito.mock(MyService.class);
when(mock.doSomething("test"))
    .thenReturn(CompletableFuture.completedFuture(result));
```

## What NOT to Suggest

❌ HTTP endpoints, REST APIs, or RESTful design
❌ Synchronous blocking methods
❌ HTTP-specific annotations (@PathParam, @QueryParam, @Body, @Header)
❌ Connection pooling (one persistent WebSocket per service)
❌ Request/response objects in method signatures (parameters are auto-serialized)

## Implementation Status

✅ **Phase 1 Complete**: Annotations and protocol design
🔄 **Phase 2 Next**: Proxy generation with reflection
⏳ **Phase 3**: WebSocket connection management
⏳ **Phase 4**: Serialization integration
⏳ **Phase 5**: Resilience patterns (circuit breaker, retries, timeouts)

## Questions to Ask Before Suggesting Code

1. Does this use WebSocket or HTTP? (Should always be WebSocket)
2. Is this method async? (Should always return CompletableFuture)
3. Are there HTTP-specific annotations? (Should never use them)
4. Is this consistent with the WebSocket protocol design?

---

**Remember**: This is a WebSocket-based RPC library, not a REST API framework. Keep it simple, async, and fast!
