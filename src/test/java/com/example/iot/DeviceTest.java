package com.example.iot;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Behaviors;

import org.junit.ClassRule;
import org.junit.Test;

import com.example.iot.DeviceManager.DeviceRegistered;
import com.example.iot.DeviceManager.ReplyDeviceList;
import com.example.iot.DeviceManager.RequestDeviceList;
import com.example.iot.DeviceManager.RequestTrackDevice;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DeviceTest {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        TestProbe<Device.TemperatureRecorded> recordProbe = testKit.createTestProbe(Device.TemperatureRecorded.class);
        TestProbe<Device.ResponseTemperature> readProbe = testKit.createTestProbe(Device.ResponseTemperature.class);
        ActorRef<Device.Command> deviceActor = testKit.spawn(Device.create("group", "device"));

        deviceActor.tell(new Device.RecordTemperature(1L, 24.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId);

        deviceActor.tell(new Device.ReadTemperature(2L, readProbe.getRef()));
        Device.ResponseTemperature response1 = readProbe.receiveMessage();
        assertEquals(2L, response1.requestId);
        assertEquals(Optional.of(24.0), response1.value);

        deviceActor.tell(new Device.RecordTemperature(3L, 55.0, recordProbe.getRef()));
        assertEquals(3L, recordProbe.receiveMessage().requestId);

        deviceActor.tell(new Device.ReadTemperature(4L, readProbe.getRef()));
        Device.ResponseTemperature response2 = readProbe.receiveMessage();
        assertEquals(4L, response2.requestId);
        assertEquals(Optional.of(55.0), response2.value);

    }

    @Test
    public void testReplyToRegistrationRequests() {
        TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
        DeviceRegistered registered1 = probe.receiveMessage();

        // another deviceId
        groupActor.tell(new RequestTrackDevice("group", "device3", probe.getRef()));
        DeviceRegistered registered2 = probe.receiveMessage();
        assertNotEquals(registered1.device, registered2.device);

        // Check that the device actors are working
        TestProbe<Device.TemperatureRecorded> recordProbe = testKit.createTestProbe(Device.TemperatureRecorded.class);
        registered1.device.tell(new Device.RecordTemperature(0L, 1.0, recordProbe.getRef()));
        assertEquals(0L, recordProbe.receiveMessage().requestId);
        registered2.device.tell(new Device.RecordTemperature(1L, 2.0, recordProbe.getRef()));
        assertEquals(1L, recordProbe.receiveMessage().requestId);
    }

    @Test
    public void testIgnoreWrongRegistrationRequests() {
        TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));
        groupActor.tell(new RequestTrackDevice("wrongGroup", "device1", probe.getRef()));
        probe.expectNoMessage();
    }

    @Test
    public void testReturnSameActorForSameDeviceId() {
        TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
        DeviceRegistered registered1 = probe.receiveMessage();

        // registering same again should be idempotent
        groupActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
        DeviceRegistered registered2 = probe.receiveMessage();
        assertEquals(registered1.device, registered2.device);
    }

    @Test
    public void testListActiveDevices() {
        TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        registeredProbe.receiveMessage();

        TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);

        groupActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
    }

    @Test
    public void testListActiveDevicesAfterOneShutsDown() {
        TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
        DeviceRegistered registered1 = registeredProbe.receiveMessage();

        groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
        DeviceRegistered registered2 = registeredProbe.receiveMessage();

        ActorRef<Device.Command> toShutDown = registered1.device;

        TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);

        groupActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);

        toShutDown.tell(Device.Passivate.INSTANCE);
        registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());

        // using awaitAssert to retry because it might take longer for the groupActor
        // to see the Terminated, that order is undefined
        registeredProbe.awaitAssert(
                () -> {
                    groupActor.tell(new RequestDeviceList(1L, "group", deviceListProbe.getRef()));
                    ReplyDeviceList r = deviceListProbe.receiveMessage();
                    assertEquals(1L, r.requestId);
                    assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids);
                    return null;
                });
    }

    @Test
    public void testDeviceManagerRequestTrackDeviceWasRegistered() {
        TestProbe<DeviceRegistered> registerdProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(new RequestTrackDevice("group", "device", registerdProbe.getRef()));
        DeviceRegistered registered1 = registerdProbe.receiveMessage();

        managerActor.tell(new RequestTrackDevice("group", "device", registerdProbe.getRef()));
        DeviceRegistered registered2 = registerdProbe.receiveMessage();

        assertEquals(registered1.device, registered2.device);

    }

    @Test
    public void testDeviceManagerRequestDeviceListOfRegisteredDevices() {
        TestProbe<DeviceRegistered> registerdProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(new RequestTrackDevice("group", "device1", registerdProbe.getRef()));
        registerdProbe.receiveMessage();
        managerActor.tell(new RequestTrackDevice("group", "device2", registerdProbe.getRef()));
        registerdProbe.receiveMessage();

        TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);
        managerActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
        ReplyDeviceList reply = deviceListProbe.receiveMessage();
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);

    }

    @Test
    public void testDeviceManagerTerminateGroup() {
        TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
        ActorRef<DeviceManager.Command> managerActor = testKit.spawn(DeviceManager.create());

        managerActor.tell(new RequestTrackDevice("group", "device", registeredProbe.getRef()));
        ActorRef<Device.Command> toShutDown = registeredProbe.receiveMessage().device;

        toShutDown.tell(Device.Passivate.INSTANCE);
        registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());

    }
}
