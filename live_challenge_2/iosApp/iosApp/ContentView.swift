import SwiftUI
import shared

struct ContentView: View {
    
	var body: some View {
		Text("")
            .onAppear {
                let httpClient = HttpClientNativeKt.createNativeHttpClient()
                let apiClient = TmbApiClient(client: httpClient)
                
                apiClient.getSubwayLines(completionHandler: { (features, error) in
                    print(features)
                })
            }
    }
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
        ContentView()
	}
}
