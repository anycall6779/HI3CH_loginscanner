package dev.local.bililight;

/** Source inventory, NOT a claim that every upstream channel has been ported. */
final class ChannelCatalog {
    static final class Entry {
        final String name,shortName,status,notes;final int color;final boolean available;
        Entry(String name,String shortName,String status,String notes,int color,boolean available){this.name=name;this.shortName=shortName;this.status=status;this.notes=notes;this.color=color;this.available=available;}
    }
    static final Entry[] ALL={
        new Entry("Bilibili","B","기기 접속 확인","중국 B服. 계정·SMS 경로와 PC 접속 사용자 확인. 소셜은 별도 검증 중이며 dispatch 자동 갱신은 미구현입니다.",0xff56d8c5,true),
        new Entry("공식 서버","官","연동 준비","원본 Official/OfficialNew 흐름이 있습니다. 현재 앱에는 공식 계정 인증·서버 구분 어댑터가 아직 이식되지 않았습니다.",0xff8aabff,false),
        new Entry("Xiaomi","MI","SDK 연동 필요","원본 Xiaomi 로그인과 mi_sdk 모듈이 있습니다. 이 앱에는 SDK 초기화·인증 콜백·채널별 dispatch 검증이 필요합니다.",0xffffad70,false),
        new Entry("UC · 九游 · 豌豆荚","UC","SDK 연동 필요","원본 UC 경로는 gundam shell SDK를 사용합니다. 채널 구분 및 SDK 콜백 이식이 필요합니다.",0xffb59af5,false),
        new Entry("Vivo","V","SDK 연동 필요","원본은 VivoUnionSDK를 사용합니다. SDK 초기화, 계정 인증, 게임 토큰 교환을 별도로 검증해야 합니다.",0xff8aabff,false),
        new Entry("Meizu · Flyme","M","원본 진입 비활성","README에는 있지만 현재 공개 LoginInstanceManager에서 Flyme 분기가 주석 처리되어 있습니다. 정상 지원으로 표시하지 않습니다.",0xff98a7ba,false),
        new Entry("OPPO","O","SDK 연동 필요","원본은 nearme GameCenterSDK를 사용합니다. 앱 등록·SDK 초기화·채널 인증 연결이 필요합니다.",0xff79dba4,false),
        new Entry("Huawei","H","SDK 연동 필요","원본은 HMS 게임/계정 SDK를 사용합니다. 해당 앱의 등록 구성과 인증 콜백 통합이 필요합니다.",0xffff8f9b,false),
        new Entry("应用宝 · QQ만","QQ","SDK 연동 필요","원본 지원 범위는 QQ입니다. WeChat을 포함하는 지원으로 확장 해석하지 않습니다. 이 앱의 Tencent 어댑터는 미구현입니다.",0xffffd178,false),
        new Entry("360","360","원본 진입 비활성","README에는 있지만 현재 공개 LoginInstanceManager의 Qihoo 진입 및 구현이 주석 처리되어 있습니다.",0xff98a7ba,false)
    };
    static Entry bili(){return ALL[0];}
}
