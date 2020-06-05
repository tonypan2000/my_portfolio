// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// import java.util.stream.*; 

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // Base cases that should return default results
    // if there is no attendee, return the whole day as available
    if (request.getAttendees().isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }
    // if the meeting request duration is longer than a day return no option
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    // excluding optional guests
    List<TimeRange> meetingTimes = new ArrayList<TimeRange>();
    // schedule for one attendee, the person initially starts free all day
    meetingTimes.add(TimeRange.WHOLE_DAY);
    Collection<String> attendees = request.getAttendees();
    // go through every event, mark each event the person's involved with 
    for (Event event : events) {
      for (String name : attendees) {
        if (event.getAttendees().contains(name)) {
          // splits the person's remaining available times
          // remove the chunk(s) that are unavailable
          for (int i = 0; i < meetingTimes.size(); i++) {
            if (event.getWhen().overlaps(meetingTimes.get(i))) {
              TimeRange currentRange = meetingTimes.get(i);
              meetingTimes.remove(i);
              // add the new split time slots
              if (currentRange.start() < event.getWhen().start() && event.getWhen().start() - currentRange.start() >= request.getDuration()) {
                meetingTimes.add(currentRange.fromStartEnd(currentRange.start(), event.getWhen().start(), false));
              }
              if (currentRange.end() > event.getWhen().end() && currentRange.end() - event.getWhen().end() >= request.getDuration()) {
                meetingTimes.add(currentRange.fromStartEnd(event.getWhen().end(), currentRange.end(), false));
              }
            }
          }
        }
      }
    }
    // TODO: Add optional guests
    // TODO: Refactor with streams
    return meetingTimes;
  }
}
