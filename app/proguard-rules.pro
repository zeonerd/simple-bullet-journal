# Add project specific ProGuard rules here.

# Glance 위젯의 ActionCallback 구현체(ToggleTaskAction, MigrateTasksAction)는
# 홈 화면 리모트뷰에서 클래스명을 통해 런타임 리플렉션으로 인스턴스화됩니다.
# R8이 이름을 바꾸거나 사용하지 않는다고 판단해 제거하면 위젯 클릭이 조용히 동작하지 않게 되므로 반드시 유지합니다.
-keep class com.simple.bulletjournal.widget.** { *; }

# Room Entity는 KSP가 생성한 코드에서 필드명으로 참조되므로 그대로 유지합니다.
-keep class com.simple.bulletjournal.data.Task { *; }
