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

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.*; 

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // Base cases that should return default results
    // if there is no attendee, return the whole day as available
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalGuests = request.getOptionalAttendees();
    if (attendees.isEmpty() && optionalGuests.isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }
    // if the meeting request duration is longer than a day return no option
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    RangeSet<Integer> possibleTimes = TreeRangeSet.create();
    possibleTimes.add(Range.closedOpen(0, 24 * 60));
    // ADD CAST TO RangeSet<Integer>
    possibleTimes.removeAll((RangeSet<Integer>)
        events.stream()
            .filter(event -> !Collections.disjoint(event.getAttendees(), attendees))
            .map(FindMeetingQuery::eventToRange)
            // ADD SPECIALIZATION TO <Integer> FOR create METHOD
            .collect(TreeRangeSet::<Integer>create,
                TreeRangeSet::add,
                TreeRangeSet::addAll));
    return possibleTimes.asRanges()
        .stream()
        .filter(time -> time.upperEndpoint() - time.lowerEndpoint() >= request.getDuration())
        .map(FindMeetingQuery::rangeToTimeRange)
        .collect(Collectors.toList());

    // TODO: Add optional guests
  }

  // helper function converts an Event to a Guava Range
  private static Range<Integer> eventToRange(Event event) {
    Range<Integer> range;
    if (event.getWhen().end() != TimeRange.END_OF_DAY) {
      range = Range.closedOpen(event.getWhen().start(), event.getWhen().end());
    } else {
      range = Range.closed(event.getWhen().start(), TimeRange.END_OF_DAY);
    }
    System.out.println("Range is " + range.toString());
    return range;
  }

  // helper function converts a Guava Range to a TimeRange
  private static TimeRange rangeToTimeRange(Range<Integer> range) {
    TimeRange timeRange;
    if (range.upperBoundType() == BoundType.OPEN) {
      timeRange = TimeRange.fromStartEnd(range.lowerEndpoint(), range.upperEndpoint(), false);
    } else {
      timeRange = TimeRange.fromStartEnd(range.lowerEndpoint(), TimeRange.END_OF_DAY, true);
    }
    return timeRange;
  }
}
