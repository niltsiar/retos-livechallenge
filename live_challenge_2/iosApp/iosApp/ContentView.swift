import SwiftUI
import shared

struct ContentView: View {
    
    @State private var subwayLines: [SubwayLine]? = nil
    @State private var selectedLine: Int? = nil
    let httpClient = HttpClientNativeKt.createNativeHttpClient()
    let apiClient: TmbApiClient
    
    init() {
        apiClient = TmbApiClient(client: httpClient)
    }
    
	var body: some View {
        guard let lines = subwayLines else {
            return AnyView(EmptyView()
                .onAppear(perform: retrieveSubwayLines)
            )
        }
        
        return AnyView(
            VStack {
                List(lines) { line in
                    SubwayLineRow(line: line, onTap: onRowTap, isSelected: line.id == selectedLine)
                }
            }
        )
    }
    
    func retrieveSubwayLines() {
        apiClient.getSubwayLines(completionHandler: { features, error in
            guard let features = features else {
                return
            }
            
            subwayLines = features.compactMap({ feature in
                guard let properties = feature.properties else {
                    return nil
                }
                return SubwayLine(lineId: Int(properties.lineCode), lineName: properties.lineName)
            })
        })
    }
    
    func onRowTap(lineCode: Int) {
        selectedLine = lineCode
    }
}

struct SubwayLineRow: View {
    
    @State private var stations: [SubwayStationProperties]? = nil
    private let line: SubwayLine
    private let onTap: (Int) -> Void
    @State private var isSelected: Bool
    
    init(line: SubwayLine, onTap: @escaping (Int) -> Void, isSelected: Bool) {
        self.line = line
        self.onTap = onTap
        self.isSelected = isSelected
    }
    
    var body: some View {
        
        HStack {
            VStack {
                Text(line.name).foregroundColor(/*@START_MENU_TOKEN@*/.blue/*@END_MENU_TOKEN@*/)
                if let stations = stations {
                    ForEach(stations, id: \.stationOrder) { station in
                        Text(station.stationName)
                    }
                } else {
                    EmptyView()
                }
            }
            Spacer()
        }
        .onChange(of: isSelected, perform: { value in
            if (isSelected) {
                let httpClient = HttpClientNativeKt.createNativeHttpClient()
                let apiClient = TmbApiClient(client: httpClient)
                apiClient.getStationFromSubwayLine(lineCode: Int32(line.id), completionHandler: { features, error in
                    guard let features = features else {
                        return
                    }
                    
                    stations = features.compactMap({ feature in feature.properties })
                })
            }
        })
        .onTapGesture {
            isSelected = true
            onTap(line.id)
        }
    }
}

class SubwayLine: Identifiable {
    let id: Int
    let name: String
    
    init(lineId: Int, lineName: String) {
        id = lineId
        name = lineName
    }
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
        ContentView()
	}
}
