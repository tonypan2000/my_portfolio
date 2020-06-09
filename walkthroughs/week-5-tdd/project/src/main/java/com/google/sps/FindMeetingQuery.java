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

import com.google.common.base.Predicates;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.*; 

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // Input sanitization / edge case, make sure the request is less than a day.
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    // 1. Get set of possible times with respect to mandatory attendees
    RangeSet<Integer> allDay = TreeRangeSet.create();
    allDay.add(Range.closedOpen(0, 24 * 60));
    // Change the signature of findSchedule to return a RangeSet.
    RangeSet<Integer> possibleTimes = findSchedule(allDay, events, request.getAttendees(), request);

    Collection<String> optionalAttendees = request.getOptionalAttendees();
    if (optionalAttendees.isEmpty()) {
      return rangeSetToTimeRangeList(possibleTimes);
    }

    // 2. Get a stream of all subsets sorted by descending cardinality.
    return allSubsetsDescending(optionalAttendees)
      // 3. Find the possible times for a given subset.
      .map(subsetOfOptionalAttendees -> findSchedule(possibleTimes, events, subsetOfOptionalAttendees, request))
      // 4. Filter out empty schedules due to impossible subsets.
      .filter(Predicates.not(RangeSet::isEmpty))
      // 5. Convert back to the desired output format.
      .map(FindMeetingQuery::rangeSetToTimeRangeList)
      // 6. Take the first element of the stream. Note: since we're streaming by descending cardinality,
      // the first element corresponds to the highest cardinality subset that had possible times.
      .findFirst()
      // 7. Retrieve the first element, or return the empty list if the stream was empty.
      .orElse(Arrays.asList());
  }

  // helper function converts an Event to a Guava Range
  private static Range<Integer> eventToRange(Event event) {
    Range<Integer> range;
    if (event.getWhen().end() != TimeRange.END_OF_DAY) {
      range = Range.closedOpen(event.getWhen().start(), event.getWhen().end());
    } else {
      range = Range.closed(event.getWhen().start(), TimeRange.END_OF_DAY);
    }
    return range;
  }

  // helper function converts Guava RangeSet to an ArrayList of TimeRanges
  private static List<TimeRange> rangeSetToTimeRangeList(RangeSet<Integer> rangeSet) {
    Set<Range<Integer>> ranges = rangeSet.asRanges();
    return ranges.stream()
        .map(FindMeetingQuery::rangeToTimeRange)
        .collect(Collectors.toList());    
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

  // helper function converting an ArrayList of TimeRange to a RangeSet of Integer
  private static RangeSet<Integer> timeRangeToRangeSet(List<TimeRange> meetingTimes) {
    RangeSet<Integer> timeRangeSet = TreeRangeSet.create();
    for (TimeRange time : meetingTimes) {
      if (time.end() != TimeRange.END_OF_DAY) {
        timeRangeSet.add(Range.closedOpen(time.start(), time.end()));
      } else {
        timeRangeSet.add(Range.closed(time.start(), TimeRange.END_OF_DAY));
      }   
    }
    return timeRangeSet;
  }

  // helper function that finds the available time slots with streams
  private static RangeSet<Integer> findSchedule(RangeSet<Integer> possibleTimes, 
      Collection<Event> events, Collection<String> attendees, MeetingRequest request) {
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
        .collect(TreeRangeSet::<Integer>create,
            TreeRangeSet::add,
            TreeRangeSet::addAll);
  }

  // helper function uses streams to build a power set
  private static Stream<Set<String>> allSubsetsDescending(Collection<String> optionalAttendees) {
    Set<String> attendeeSet = Sets.newHashSet(optionalAttendees);
    return IntStream.range(0, attendeeSet.size())
        .boxed() // Converts Intstream to Stream<Integer>
        .sorted(Collections.reverseOrder())
				.flatMap(cardinality ->
            Stream.concat(Stream.of(Sets.combinations(attendeeSet, cardinality)), Stream.of(Collections.<String>emptySet())));
  }
}
