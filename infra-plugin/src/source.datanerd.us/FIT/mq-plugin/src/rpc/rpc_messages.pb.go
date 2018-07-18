// Code generated by protoc-gen-go. DO NOT EDIT.
// source: rpc_messages.proto

package rpc

import proto "github.com/golang/protobuf/proto"
import fmt "fmt"
import math "math"

import (
	context "golang.org/x/net/context"
	grpc "google.golang.org/grpc"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion2 // please upgrade the proto package

type SourceType int32

const (
	SourceType_GAUGE     SourceType = 0
	SourceType_ATTRIBUTE SourceType = 1
	SourceType_RATE      SourceType = 2
	SourceType_DELTA     SourceType = 3
)

var SourceType_name = map[int32]string{
	0: "GAUGE",
	1: "ATTRIBUTE",
	2: "RATE",
	3: "DELTA",
}
var SourceType_value = map[string]int32{
	"GAUGE":     0,
	"ATTRIBUTE": 1,
	"RATE":      2,
	"DELTA":     3,
}

func (x SourceType) String() string {
	return proto.EnumName(SourceType_name, int32(x))
}
func (SourceType) EnumDescriptor() ([]byte, []int) {
	return fileDescriptor_rpc_messages_45c7334a8476bc02, []int{0}
}

type MetricData struct {
	MetricName string `protobuf:"bytes,1,opt,name=metric_name,json=metricName,proto3" json:"metric_name,omitempty"`
	// Types that are valid to be assigned to MetricValue:
	//	*MetricData_BooleanValue
	//	*MetricData_IntValue
	//	*MetricData_LongValue
	//	*MetricData_DoubleValue
	//	*MetricData_FloatValue
	//	*MetricData_StringValue
	MetricValue          isMetricData_MetricValue `protobuf_oneof:"metric_value"`
	SourceType           SourceType               `protobuf:"varint,9,opt,name=source_type,json=sourceType,proto3,enum=SourceType" json:"source_type,omitempty"`
	XXX_NoUnkeyedLiteral struct{}                 `json:"-"`
	XXX_unrecognized     []byte                   `json:"-"`
	XXX_sizecache        int32                    `json:"-"`
}

func (m *MetricData) Reset()         { *m = MetricData{} }
func (m *MetricData) String() string { return proto.CompactTextString(m) }
func (*MetricData) ProtoMessage()    {}
func (*MetricData) Descriptor() ([]byte, []int) {
	return fileDescriptor_rpc_messages_45c7334a8476bc02, []int{0}
}
func (m *MetricData) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_MetricData.Unmarshal(m, b)
}
func (m *MetricData) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_MetricData.Marshal(b, m, deterministic)
}
func (dst *MetricData) XXX_Merge(src proto.Message) {
	xxx_messageInfo_MetricData.Merge(dst, src)
}
func (m *MetricData) XXX_Size() int {
	return xxx_messageInfo_MetricData.Size(m)
}
func (m *MetricData) XXX_DiscardUnknown() {
	xxx_messageInfo_MetricData.DiscardUnknown(m)
}

var xxx_messageInfo_MetricData proto.InternalMessageInfo

type isMetricData_MetricValue interface {
	isMetricData_MetricValue()
}

type MetricData_BooleanValue struct {
	BooleanValue bool `protobuf:"varint,3,opt,name=boolean_value,json=booleanValue,proto3,oneof"`
}
type MetricData_IntValue struct {
	IntValue int32 `protobuf:"varint,4,opt,name=int_value,json=intValue,proto3,oneof"`
}
type MetricData_LongValue struct {
	LongValue int64 `protobuf:"varint,5,opt,name=long_value,json=longValue,proto3,oneof"`
}
type MetricData_DoubleValue struct {
	DoubleValue float64 `protobuf:"fixed64,6,opt,name=double_value,json=doubleValue,proto3,oneof"`
}
type MetricData_FloatValue struct {
	FloatValue float32 `protobuf:"fixed32,7,opt,name=float_value,json=floatValue,proto3,oneof"`
}
type MetricData_StringValue struct {
	StringValue string `protobuf:"bytes,8,opt,name=string_value,json=stringValue,proto3,oneof"`
}

func (*MetricData_BooleanValue) isMetricData_MetricValue() {}
func (*MetricData_IntValue) isMetricData_MetricValue()     {}
func (*MetricData_LongValue) isMetricData_MetricValue()    {}
func (*MetricData_DoubleValue) isMetricData_MetricValue()  {}
func (*MetricData_FloatValue) isMetricData_MetricValue()   {}
func (*MetricData_StringValue) isMetricData_MetricValue()  {}

func (m *MetricData) GetMetricValue() isMetricData_MetricValue {
	if m != nil {
		return m.MetricValue
	}
	return nil
}

func (m *MetricData) GetMetricName() string {
	if m != nil {
		return m.MetricName
	}
	return ""
}

func (m *MetricData) GetBooleanValue() bool {
	if x, ok := m.GetMetricValue().(*MetricData_BooleanValue); ok {
		return x.BooleanValue
	}
	return false
}

func (m *MetricData) GetIntValue() int32 {
	if x, ok := m.GetMetricValue().(*MetricData_IntValue); ok {
		return x.IntValue
	}
	return 0
}

func (m *MetricData) GetLongValue() int64 {
	if x, ok := m.GetMetricValue().(*MetricData_LongValue); ok {
		return x.LongValue
	}
	return 0
}

func (m *MetricData) GetDoubleValue() float64 {
	if x, ok := m.GetMetricValue().(*MetricData_DoubleValue); ok {
		return x.DoubleValue
	}
	return 0
}

func (m *MetricData) GetFloatValue() float32 {
	if x, ok := m.GetMetricValue().(*MetricData_FloatValue); ok {
		return x.FloatValue
	}
	return 0
}

func (m *MetricData) GetStringValue() string {
	if x, ok := m.GetMetricValue().(*MetricData_StringValue); ok {
		return x.StringValue
	}
	return ""
}

func (m *MetricData) GetSourceType() SourceType {
	if m != nil {
		return m.SourceType
	}
	return SourceType_GAUGE
}

// XXX_OneofFuncs is for the internal use of the proto package.
func (*MetricData) XXX_OneofFuncs() (func(msg proto.Message, b *proto.Buffer) error, func(msg proto.Message, tag, wire int, b *proto.Buffer) (bool, error), func(msg proto.Message) (n int), []interface{}) {
	return _MetricData_OneofMarshaler, _MetricData_OneofUnmarshaler, _MetricData_OneofSizer, []interface{}{
		(*MetricData_BooleanValue)(nil),
		(*MetricData_IntValue)(nil),
		(*MetricData_LongValue)(nil),
		(*MetricData_DoubleValue)(nil),
		(*MetricData_FloatValue)(nil),
		(*MetricData_StringValue)(nil),
	}
}

func _MetricData_OneofMarshaler(msg proto.Message, b *proto.Buffer) error {
	m := msg.(*MetricData)
	// metric_value
	switch x := m.MetricValue.(type) {
	case *MetricData_BooleanValue:
		t := uint64(0)
		if x.BooleanValue {
			t = 1
		}
		b.EncodeVarint(3<<3 | proto.WireVarint)
		b.EncodeVarint(t)
	case *MetricData_IntValue:
		b.EncodeVarint(4<<3 | proto.WireVarint)
		b.EncodeVarint(uint64(x.IntValue))
	case *MetricData_LongValue:
		b.EncodeVarint(5<<3 | proto.WireVarint)
		b.EncodeVarint(uint64(x.LongValue))
	case *MetricData_DoubleValue:
		b.EncodeVarint(6<<3 | proto.WireFixed64)
		b.EncodeFixed64(math.Float64bits(x.DoubleValue))
	case *MetricData_FloatValue:
		b.EncodeVarint(7<<3 | proto.WireFixed32)
		b.EncodeFixed32(uint64(math.Float32bits(x.FloatValue)))
	case *MetricData_StringValue:
		b.EncodeVarint(8<<3 | proto.WireBytes)
		b.EncodeStringBytes(x.StringValue)
	case nil:
	default:
		return fmt.Errorf("MetricData.MetricValue has unexpected type %T", x)
	}
	return nil
}

func _MetricData_OneofUnmarshaler(msg proto.Message, tag, wire int, b *proto.Buffer) (bool, error) {
	m := msg.(*MetricData)
	switch tag {
	case 3: // metric_value.boolean_value
		if wire != proto.WireVarint {
			return true, proto.ErrInternalBadWireType
		}
		x, err := b.DecodeVarint()
		m.MetricValue = &MetricData_BooleanValue{x != 0}
		return true, err
	case 4: // metric_value.int_value
		if wire != proto.WireVarint {
			return true, proto.ErrInternalBadWireType
		}
		x, err := b.DecodeVarint()
		m.MetricValue = &MetricData_IntValue{int32(x)}
		return true, err
	case 5: // metric_value.long_value
		if wire != proto.WireVarint {
			return true, proto.ErrInternalBadWireType
		}
		x, err := b.DecodeVarint()
		m.MetricValue = &MetricData_LongValue{int64(x)}
		return true, err
	case 6: // metric_value.double_value
		if wire != proto.WireFixed64 {
			return true, proto.ErrInternalBadWireType
		}
		x, err := b.DecodeFixed64()
		m.MetricValue = &MetricData_DoubleValue{math.Float64frombits(x)}
		return true, err
	case 7: // metric_value.float_value
		if wire != proto.WireFixed32 {
			return true, proto.ErrInternalBadWireType
		}
		x, err := b.DecodeFixed32()
		m.MetricValue = &MetricData_FloatValue{math.Float32frombits(uint32(x))}
		return true, err
	case 8: // metric_value.string_value
		if wire != proto.WireBytes {
			return true, proto.ErrInternalBadWireType
		}
		x, err := b.DecodeStringBytes()
		m.MetricValue = &MetricData_StringValue{x}
		return true, err
	default:
		return false, nil
	}
}

func _MetricData_OneofSizer(msg proto.Message) (n int) {
	m := msg.(*MetricData)
	// metric_value
	switch x := m.MetricValue.(type) {
	case *MetricData_BooleanValue:
		n += 1 // tag and wire
		n += 1
	case *MetricData_IntValue:
		n += 1 // tag and wire
		n += proto.SizeVarint(uint64(x.IntValue))
	case *MetricData_LongValue:
		n += 1 // tag and wire
		n += proto.SizeVarint(uint64(x.LongValue))
	case *MetricData_DoubleValue:
		n += 1 // tag and wire
		n += 8
	case *MetricData_FloatValue:
		n += 1 // tag and wire
		n += 4
	case *MetricData_StringValue:
		n += 1 // tag and wire
		n += proto.SizeVarint(uint64(len(x.StringValue)))
		n += len(x.StringValue)
	case nil:
	default:
		panic(fmt.Sprintf("proto: unexpected type %T in oneof", x))
	}
	return n
}

type MetricRequest struct {
	Args                 map[string]string `protobuf:"bytes,1,rep,name=args,proto3" json:"args,omitempty" protobuf_key:"bytes,1,opt,name=key,proto3" protobuf_val:"bytes,2,opt,name=value,proto3"`
	XXX_NoUnkeyedLiteral struct{}          `json:"-"`
	XXX_unrecognized     []byte            `json:"-"`
	XXX_sizecache        int32             `json:"-"`
}

func (m *MetricRequest) Reset()         { *m = MetricRequest{} }
func (m *MetricRequest) String() string { return proto.CompactTextString(m) }
func (*MetricRequest) ProtoMessage()    {}
func (*MetricRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_rpc_messages_45c7334a8476bc02, []int{1}
}
func (m *MetricRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_MetricRequest.Unmarshal(m, b)
}
func (m *MetricRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_MetricRequest.Marshal(b, m, deterministic)
}
func (dst *MetricRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_MetricRequest.Merge(dst, src)
}
func (m *MetricRequest) XXX_Size() int {
	return xxx_messageInfo_MetricRequest.Size(m)
}
func (m *MetricRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_MetricRequest.DiscardUnknown(m)
}

var xxx_messageInfo_MetricRequest proto.InternalMessageInfo

func (m *MetricRequest) GetArgs() map[string]string {
	if m != nil {
		return m.Args
	}
	return nil
}

type MetricResponse struct {
	MetricSetName        string        `protobuf:"bytes,1,opt,name=metric_set_name,json=metricSetName,proto3" json:"metric_set_name,omitempty"`
	MetricSet            []*MetricData `protobuf:"bytes,2,rep,name=metric_set,json=metricSet,proto3" json:"metric_set,omitempty"`
	XXX_NoUnkeyedLiteral struct{}      `json:"-"`
	XXX_unrecognized     []byte        `json:"-"`
	XXX_sizecache        int32         `json:"-"`
}

func (m *MetricResponse) Reset()         { *m = MetricResponse{} }
func (m *MetricResponse) String() string { return proto.CompactTextString(m) }
func (*MetricResponse) ProtoMessage()    {}
func (*MetricResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_rpc_messages_45c7334a8476bc02, []int{2}
}
func (m *MetricResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_MetricResponse.Unmarshal(m, b)
}
func (m *MetricResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_MetricResponse.Marshal(b, m, deterministic)
}
func (dst *MetricResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_MetricResponse.Merge(dst, src)
}
func (m *MetricResponse) XXX_Size() int {
	return xxx_messageInfo_MetricResponse.Size(m)
}
func (m *MetricResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_MetricResponse.DiscardUnknown(m)
}

var xxx_messageInfo_MetricResponse proto.InternalMessageInfo

func (m *MetricResponse) GetMetricSetName() string {
	if m != nil {
		return m.MetricSetName
	}
	return ""
}

func (m *MetricResponse) GetMetricSet() []*MetricData {
	if m != nil {
		return m.MetricSet
	}
	return nil
}

type InventoryRequest struct {
	Args                 map[string]string `protobuf:"bytes,1,rep,name=args,proto3" json:"args,omitempty" protobuf_key:"bytes,1,opt,name=key,proto3" protobuf_val:"bytes,2,opt,name=value,proto3"`
	XXX_NoUnkeyedLiteral struct{}          `json:"-"`
	XXX_unrecognized     []byte            `json:"-"`
	XXX_sizecache        int32             `json:"-"`
}

func (m *InventoryRequest) Reset()         { *m = InventoryRequest{} }
func (m *InventoryRequest) String() string { return proto.CompactTextString(m) }
func (*InventoryRequest) ProtoMessage()    {}
func (*InventoryRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_rpc_messages_45c7334a8476bc02, []int{3}
}
func (m *InventoryRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_InventoryRequest.Unmarshal(m, b)
}
func (m *InventoryRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_InventoryRequest.Marshal(b, m, deterministic)
}
func (dst *InventoryRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_InventoryRequest.Merge(dst, src)
}
func (m *InventoryRequest) XXX_Size() int {
	return xxx_messageInfo_InventoryRequest.Size(m)
}
func (m *InventoryRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_InventoryRequest.DiscardUnknown(m)
}

var xxx_messageInfo_InventoryRequest proto.InternalMessageInfo

func (m *InventoryRequest) GetArgs() map[string]string {
	if m != nil {
		return m.Args
	}
	return nil
}

type InventoryResponse struct {
	InventoryName        string            `protobuf:"bytes,1,opt,name=inventory_name,json=inventoryName,proto3" json:"inventory_name,omitempty"`
	Inventory            map[string]string `protobuf:"bytes,2,rep,name=inventory,proto3" json:"inventory,omitempty" protobuf_key:"bytes,1,opt,name=key,proto3" protobuf_val:"bytes,2,opt,name=value,proto3"`
	XXX_NoUnkeyedLiteral struct{}          `json:"-"`
	XXX_unrecognized     []byte            `json:"-"`
	XXX_sizecache        int32             `json:"-"`
}

func (m *InventoryResponse) Reset()         { *m = InventoryResponse{} }
func (m *InventoryResponse) String() string { return proto.CompactTextString(m) }
func (*InventoryResponse) ProtoMessage()    {}
func (*InventoryResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_rpc_messages_45c7334a8476bc02, []int{4}
}
func (m *InventoryResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_InventoryResponse.Unmarshal(m, b)
}
func (m *InventoryResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_InventoryResponse.Marshal(b, m, deterministic)
}
func (dst *InventoryResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_InventoryResponse.Merge(dst, src)
}
func (m *InventoryResponse) XXX_Size() int {
	return xxx_messageInfo_InventoryResponse.Size(m)
}
func (m *InventoryResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_InventoryResponse.DiscardUnknown(m)
}

var xxx_messageInfo_InventoryResponse proto.InternalMessageInfo

func (m *InventoryResponse) GetInventoryName() string {
	if m != nil {
		return m.InventoryName
	}
	return ""
}

func (m *InventoryResponse) GetInventory() map[string]string {
	if m != nil {
		return m.Inventory
	}
	return nil
}

func init() {
	proto.RegisterType((*MetricData)(nil), "MetricData")
	proto.RegisterType((*MetricRequest)(nil), "MetricRequest")
	proto.RegisterMapType((map[string]string)(nil), "MetricRequest.ArgsEntry")
	proto.RegisterType((*MetricResponse)(nil), "MetricResponse")
	proto.RegisterType((*InventoryRequest)(nil), "InventoryRequest")
	proto.RegisterMapType((map[string]string)(nil), "InventoryRequest.ArgsEntry")
	proto.RegisterType((*InventoryResponse)(nil), "InventoryResponse")
	proto.RegisterMapType((map[string]string)(nil), "InventoryResponse.InventoryEntry")
	proto.RegisterEnum("SourceType", SourceType_name, SourceType_value)
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConn

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion4

// PluginServiceClient is the client API for PluginService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type PluginServiceClient interface {
	GetMetrics(ctx context.Context, in *MetricRequest, opts ...grpc.CallOption) (PluginService_GetMetricsClient, error)
	GetInventory(ctx context.Context, in *InventoryRequest, opts ...grpc.CallOption) (PluginService_GetInventoryClient, error)
}

type pluginServiceClient struct {
	cc *grpc.ClientConn
}

func NewPluginServiceClient(cc *grpc.ClientConn) PluginServiceClient {
	return &pluginServiceClient{cc}
}

func (c *pluginServiceClient) GetMetrics(ctx context.Context, in *MetricRequest, opts ...grpc.CallOption) (PluginService_GetMetricsClient, error) {
	stream, err := c.cc.NewStream(ctx, &_PluginService_serviceDesc.Streams[0], "/PluginService/GetMetrics", opts...)
	if err != nil {
		return nil, err
	}
	x := &pluginServiceGetMetricsClient{stream}
	if err := x.ClientStream.SendMsg(in); err != nil {
		return nil, err
	}
	if err := x.ClientStream.CloseSend(); err != nil {
		return nil, err
	}
	return x, nil
}

type PluginService_GetMetricsClient interface {
	Recv() (*MetricResponse, error)
	grpc.ClientStream
}

type pluginServiceGetMetricsClient struct {
	grpc.ClientStream
}

func (x *pluginServiceGetMetricsClient) Recv() (*MetricResponse, error) {
	m := new(MetricResponse)
	if err := x.ClientStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

func (c *pluginServiceClient) GetInventory(ctx context.Context, in *InventoryRequest, opts ...grpc.CallOption) (PluginService_GetInventoryClient, error) {
	stream, err := c.cc.NewStream(ctx, &_PluginService_serviceDesc.Streams[1], "/PluginService/GetInventory", opts...)
	if err != nil {
		return nil, err
	}
	x := &pluginServiceGetInventoryClient{stream}
	if err := x.ClientStream.SendMsg(in); err != nil {
		return nil, err
	}
	if err := x.ClientStream.CloseSend(); err != nil {
		return nil, err
	}
	return x, nil
}

type PluginService_GetInventoryClient interface {
	Recv() (*InventoryResponse, error)
	grpc.ClientStream
}

type pluginServiceGetInventoryClient struct {
	grpc.ClientStream
}

func (x *pluginServiceGetInventoryClient) Recv() (*InventoryResponse, error) {
	m := new(InventoryResponse)
	if err := x.ClientStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

// PluginServiceServer is the server API for PluginService service.
type PluginServiceServer interface {
	GetMetrics(*MetricRequest, PluginService_GetMetricsServer) error
	GetInventory(*InventoryRequest, PluginService_GetInventoryServer) error
}

func RegisterPluginServiceServer(s *grpc.Server, srv PluginServiceServer) {
	s.RegisterService(&_PluginService_serviceDesc, srv)
}

func _PluginService_GetMetrics_Handler(srv interface{}, stream grpc.ServerStream) error {
	m := new(MetricRequest)
	if err := stream.RecvMsg(m); err != nil {
		return err
	}
	return srv.(PluginServiceServer).GetMetrics(m, &pluginServiceGetMetricsServer{stream})
}

type PluginService_GetMetricsServer interface {
	Send(*MetricResponse) error
	grpc.ServerStream
}

type pluginServiceGetMetricsServer struct {
	grpc.ServerStream
}

func (x *pluginServiceGetMetricsServer) Send(m *MetricResponse) error {
	return x.ServerStream.SendMsg(m)
}

func _PluginService_GetInventory_Handler(srv interface{}, stream grpc.ServerStream) error {
	m := new(InventoryRequest)
	if err := stream.RecvMsg(m); err != nil {
		return err
	}
	return srv.(PluginServiceServer).GetInventory(m, &pluginServiceGetInventoryServer{stream})
}

type PluginService_GetInventoryServer interface {
	Send(*InventoryResponse) error
	grpc.ServerStream
}

type pluginServiceGetInventoryServer struct {
	grpc.ServerStream
}

func (x *pluginServiceGetInventoryServer) Send(m *InventoryResponse) error {
	return x.ServerStream.SendMsg(m)
}

var _PluginService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "PluginService",
	HandlerType: (*PluginServiceServer)(nil),
	Methods:     []grpc.MethodDesc{},
	Streams: []grpc.StreamDesc{
		{
			StreamName:    "GetMetrics",
			Handler:       _PluginService_GetMetrics_Handler,
			ServerStreams: true,
		},
		{
			StreamName:    "GetInventory",
			Handler:       _PluginService_GetInventory_Handler,
			ServerStreams: true,
		},
	},
	Metadata: "rpc_messages.proto",
}

func init() { proto.RegisterFile("rpc_messages.proto", fileDescriptor_rpc_messages_45c7334a8476bc02) }

var fileDescriptor_rpc_messages_45c7334a8476bc02 = []byte{
	// 581 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0xac, 0x94, 0x41, 0x6b, 0xdb, 0x4c,
	0x10, 0x86, 0xbd, 0x76, 0x9c, 0xcf, 0x1a, 0xd9, 0x8e, 0xb3, 0x7c, 0x07, 0xe3, 0x52, 0xa2, 0xb8,
	0xa4, 0x88, 0x10, 0x36, 0x21, 0x3d, 0xa4, 0xb4, 0x85, 0x62, 0x37, 0xc6, 0x0e, 0x34, 0xa5, 0x6c,
	0x9c, 0x1e, 0x7a, 0x31, 0xb2, 0x32, 0x71, 0x45, 0xe5, 0x95, 0xba, 0xbb, 0x76, 0x11, 0xf4, 0x8f,
	0xf5, 0xd4, 0xbf, 0x56, 0xa4, 0x95, 0xe5, 0xa8, 0x39, 0x15, 0x7a, 0xd3, 0x3e, 0xf3, 0x6a, 0x76,
	0xde, 0x99, 0x61, 0x81, 0xca, 0xd8, 0x9f, 0x2d, 0x51, 0x29, 0x6f, 0x81, 0x8a, 0xc5, 0x32, 0xd2,
	0x51, 0xff, 0x57, 0x15, 0xe0, 0x1a, 0xb5, 0x0c, 0xfc, 0x4b, 0x4f, 0x7b, 0xf4, 0x00, 0xec, 0x65,
	0x76, 0x9a, 0x09, 0x6f, 0x89, 0x5d, 0xe2, 0x10, 0xd7, 0xe2, 0x60, 0xd0, 0x07, 0x6f, 0x89, 0xf4,
	0x08, 0x5a, 0xf3, 0x28, 0x0a, 0xd1, 0x13, 0xb3, 0xb5, 0x17, 0xae, 0xb0, 0x5b, 0x73, 0x88, 0xdb,
	0x98, 0x54, 0x78, 0x33, 0xc7, 0x9f, 0x52, 0x4a, 0x9f, 0x82, 0x15, 0x08, 0x9d, 0x4b, 0x76, 0x1c,
	0xe2, 0xd6, 0x27, 0x15, 0xde, 0x08, 0x84, 0x36, 0xe1, 0x03, 0x80, 0x30, 0x12, 0x8b, 0x3c, 0x5e,
	0x77, 0x88, 0x5b, 0x9b, 0x54, 0xb8, 0x95, 0x32, 0x23, 0x78, 0x06, 0xcd, 0xbb, 0x68, 0x35, 0x0f,
	0x31, 0x97, 0xec, 0x3a, 0xc4, 0x25, 0x93, 0x0a, 0xb7, 0x0d, 0x35, 0xa2, 0x43, 0xb0, 0xef, 0xc3,
	0xc8, 0xdb, 0x5c, 0xf3, 0x9f, 0x43, 0xdc, 0xea, 0xa4, 0xc2, 0x21, 0x83, 0x45, 0x1e, 0xa5, 0x65,
	0x50, 0x5c, 0xd5, 0x48, 0x0d, 0xa5, 0x79, 0x0c, 0x35, 0xa2, 0x13, 0xb0, 0x55, 0xb4, 0x92, 0x3e,
	0xce, 0x74, 0x12, 0x63, 0xd7, 0x72, 0x88, 0xdb, 0x3e, 0xb7, 0xd9, 0x4d, 0xc6, 0xa6, 0x49, 0x8c,
	0x1c, 0x54, 0xf1, 0x3d, 0x6c, 0x43, 0x33, 0x6f, 0x51, 0x96, 0xb2, 0xbf, 0x86, 0x96, 0x69, 0x20,
	0xc7, 0x6f, 0x2b, 0x54, 0x9a, 0x9e, 0xc0, 0x8e, 0x27, 0x17, 0xaa, 0x4b, 0x9c, 0x9a, 0x6b, 0x9f,
	0x77, 0x59, 0x29, 0xca, 0x06, 0x72, 0xa1, 0x46, 0x42, 0xcb, 0x84, 0x67, 0xaa, 0xde, 0x05, 0x58,
	0x05, 0xa2, 0x1d, 0xa8, 0x7d, 0xc5, 0x24, 0x6f, 0x7b, 0xfa, 0x49, 0xff, 0x87, 0xba, 0xa9, 0xbc,
	0x9a, 0x31, 0x73, 0x78, 0x55, 0x7d, 0x49, 0xfa, 0x77, 0xd0, 0xde, 0x64, 0x56, 0x71, 0x24, 0x14,
	0xd2, 0xe7, 0xb0, 0x97, 0x57, 0xa6, 0x50, 0x3f, 0x1c, 0x60, 0xcb, 0xe0, 0x1b, 0xd4, 0xd9, 0x0c,
	0x8f, 0x01, 0xb6, 0xba, 0x6e, 0x35, 0x2b, 0xd3, 0x66, 0xdb, 0x2d, 0xe0, 0x56, 0xa1, 0xef, 0xff,
	0x80, 0xce, 0x95, 0x58, 0xa3, 0xd0, 0x91, 0x4c, 0x36, 0x06, 0x4f, 0x4b, 0x06, 0x9f, 0xb0, 0x3f,
	0x05, 0xff, 0xce, 0xe3, 0x4f, 0x02, 0xfb, 0x0f, 0xb2, 0xe7, 0x3e, 0x8f, 0xa0, 0x1d, 0x6c, 0x60,
	0xc9, 0x66, 0x41, 0x33, 0x9b, 0x6f, 0xd3, 0x1d, 0xcc, 0x41, 0xee, 0xf2, 0x90, 0x3d, 0xca, 0xb6,
	0x25, 0xa6, 0xe2, 0xed, 0x3f, 0xbd, 0x37, 0xd0, 0x2e, 0x07, 0xff, 0xa6, 0xf6, 0xe3, 0xd7, 0x00,
	0xdb, 0x0d, 0xa2, 0x16, 0xd4, 0xc7, 0x83, 0xdb, 0xf1, 0xa8, 0x53, 0xa1, 0x2d, 0xb0, 0x06, 0xd3,
	0x29, 0xbf, 0x1a, 0xde, 0x4e, 0x47, 0x1d, 0x42, 0x1b, 0xb0, 0xc3, 0x07, 0xd3, 0x51, 0xa7, 0x9a,
	0x6a, 0x2e, 0x47, 0xef, 0xa7, 0x83, 0x4e, 0xed, 0x3c, 0x81, 0xd6, 0xc7, 0x70, 0xb5, 0x08, 0xc4,
	0x0d, 0xca, 0x75, 0xe0, 0x23, 0x3d, 0x05, 0x18, 0xa3, 0x36, 0x33, 0x52, 0xb4, 0x5d, 0x5e, 0xaa,
	0xde, 0x1e, 0x2b, 0xaf, 0xc2, 0x19, 0xa1, 0x17, 0xd0, 0x1c, 0xa3, 0x2e, 0xea, 0xa7, 0xfb, 0x8f,
	0xc6, 0xd4, 0xa3, 0x8f, 0xbb, 0x71, 0x46, 0x86, 0xef, 0xe0, 0xd4, 0x8f, 0x96, 0x4c, 0xe0, 0x77,
	0x89, 0x61, 0xe0, 0xb3, 0x40, 0xdc, 0x4b, 0x8f, 0x99, 0x8d, 0x50, 0x2c, 0x5e, 0xcd, 0xc3, 0x40,
	0x7d, 0x61, 0x32, 0xf6, 0xd9, 0xe6, 0x29, 0x19, 0xda, 0x3c, 0xf6, 0xaf, 0xf3, 0xc3, 0xe7, 0x9a,
	0x8c, 0xfd, 0xf9, 0x6e, 0xf6, 0xba, 0xbc, 0xf8, 0x1d, 0x00, 0x00, 0xff, 0xff, 0x6f, 0x86, 0xf9,
	0x48, 0x73, 0x04, 0x00, 0x00,
}
