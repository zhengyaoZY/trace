import SwiftUI

@main
struct TraceApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .tint(.accentColor)
                .environment(\.locale, Locale(identifier: "zh_Hans_CN"))
        }
    }
}
